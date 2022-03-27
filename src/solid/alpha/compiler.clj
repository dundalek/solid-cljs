(ns solid.alpha.compiler
  (:require [clojure.string :as str]
            [clojure.tools.analyzer.passes :as ana.passes]
            [clojure.walk]))

(def ^:dynamic *templates* nil)

(defn escape [s]
  ;; TODO use proper escaping, see dom-expressions/server.js escape
  (str/replace s "<" "&lt;"))

(defn escape-attr-name [s]
  ;; TODO escape
  s)

(defn escape-attr-value [s]
  ;; TODO escape
  (str "\"" s "\""))

(defn primitive? [x]
  (or (nil? x)
      (false? x)
      (number? x)
      (string? x)))

(defn trim [f coll]
  ;; TODO something less naive
  (->> coll
       (drop-while f)
       (reverse)
       (drop-while f)
       (reverse)))

(defn process-child [child]
  (cond (string? child) (escape child)
        (number? child) (str child)
        (or (nil? child) (false? child)) nil
        :else "<!>"))

(defn compile-children [coll]
  (->> coll
       (trim (complement primitive?)) ; inserted values around edges don't need placeholder
                    ;; todo flatten multiple placeholders into one
       (map process-child)
       (apply str)))

(defn compile-tag [el props children]
  (cond (keyword? el)
        (let [tag (name el)
              props (when (seq props)
                      (str " " (->> props
                                    (map (fn [[k v]]
                                           (let [k-str (if (keyword? k) (name k) k)]
                                             (str (escape-attr-name k-str) "=" (escape-attr-value v)))))
                                    (str/join " "))))]
          (str "<" tag props ">"
               children
               "</" tag ">"))))

(defn props->ops [el-sym props]
  (->> props
       (map (fn [[k v]]
              (let [k-str (if (keyword? k) (name k) k)]
                (cond
                  (= k :class) (let [assig-expr `(goog.object/set ~el-sym "className" ~v)]
                                 (if (symbol? v)
                                   assig-expr
                                   `(solid.alpha.web/effect
                                     (~'fn [] ~assig-expr))))

                  (str/starts-with? k-str "on")
                  (let [event-name (-> k-str
                                       (str/replace #"^on-?" "")
                                       (str/lower-case))]
                    `(.addEventListener ~el-sym ~event-name ~v))

                  :else `(goog.object/set ~el-sym ~k-str ~v)))))
       (into [])))

(defmacro compile-static [el & body]
  (compile-tag el nil (compile-children body)))

(defn compilable-template? [form]
  (and (seq? form)
       (#{'$ 'compile-template 'solid.alpha.compiler/compile-template} (first form))))

(declare analyze)
(declare emit)

(defn compile-element
  {:pass-info {:walk :post :depends #{} :after #{}}}
  [{:keys [op tag args] :as ast}]
  (if (not= op :element)
    ast
    (let [props (when (and (= :expression (:op (first args)))
                           (map? (:val (first args))))
                  (:val (first args)))
          children (if props
                     (rest args)
                     args)
          props-divided (->> props
                             (group-by (fn [[_k v]]
                                         (primitive? v))))
          static-props (get props-divided true)
          dynamic-props (get props-divided false)
          root-sym (gensym "el")
          first-child-sym (gensym "el")
          prop-ops (props->ops root-sym dynamic-props)

          {:keys [template bindings ops]}
          (reduce
           (fn [acc node]
             (let [next-el-sym (gensym "el")]
               (-> (case (:op node)
                     :literal (cond-> acc
                                :always (update :template str (:val node))
                                (not= :literal (:previous-op acc)) (->
                                                                    (update :bindings into [next-el-sym `(.-nextSibling ~(:el-sym acc))])
                                                                    (assoc :el-sym next-el-sym)))
                     :element (-> acc
                                  (update :template str (:template node))
                                  (update :bindings into [(:el-sym node) (:el-sym acc)
                                                          next-el-sym `(.-nextSibling ~(:el-sym node))])
                                  (update :bindings into (:bindings node))
                                  (update :ops into (:ops node))
                                  (assoc :el-sym next-el-sym))
                     (-> acc
                         (update :template str "<!>")
                         (update :ops conj `(.call solid.alpha.web/insert nil ~root-sym ~(emit node) ~(:el-sym acc)))
                         (update :bindings into [next-el-sym `(.-nextSibling ~(:el-sym acc))])
                         (assoc :el-sym next-el-sym)))
                   (assoc :previous-op (:op node)))))

                 ;; TODO elements, components, fragments without effect
           {:template ""
            :bindings [first-child-sym `(.-firstChild ~root-sym)]
            :ops []
            :el-sym first-child-sym}
           children)]
      {:op :element
       :template (compile-tag tag static-props template)
       :args args
       :el-sym root-sym
       :bindings (-> bindings pop (conj nil))
       :ops (into prop-ops ops)})))

(defn analyze-element [[_ tag & args]]
  {:op :element
   :tag tag
   :args (->> args (map analyze) vec)
   :children [:args]})
   ; :form form})

(defn analyze [form]
  (cond (primitive? form)
        {:op :literal
         :val form}
         ; :form form}

        (and (compilable-template? form)
             (symbol? (second form)))
        {:op :component
         :component (second form)
         :args (->> form rest rest (map analyze) vec)
         :children [:args]}
         ; :form form

        (and (compilable-template? form)
             (= :<> (second form)))
        {:op :fragment
         :args (->> form rest rest (map analyze) vec)
         :children [:args]}

        (and (compilable-template? form)
             (string? (second form)))
        (analyze-element form)

        (and (compilable-template? form)
             (keyword? (second form)))
        (analyze-element form)

        :else
        {:op :expression
         :val form}))
         ; :form form}))

(def run-passes (ana.passes/schedule #{#'compile-element}))

(defn emit-element [{:keys [el-sym template bindings ops]}]
  (let [tmpl-sym (gensym "tmpl")
        tmpl-expr `(.call solid.alpha.web/template nil ~template)]
    (when *templates*
      (swap! *templates* assoc tmpl-sym tmpl-expr))
    (list `(fn []
             (let [~@(when-not *templates*
                       [tmpl-sym tmpl-expr])
                   ~el-sym (.cloneNode ~tmpl-sym true)
                   ~@bindings]
               ~@ops
               ~el-sym)))))

(defn emit
  [{:keys [op] :as ast}]
  (case op
    :element (emit-element ast)
    :fragment `(cljs.core/array ~@(->> ast :args (map emit)))
    :component `(.call solid.alpha.web/create-component nil ~(:component ast) ~@(->> ast :args (map emit)))
    :expression (:val ast)
    :literal (str (:val ast))))

(defn compile-template* [form]
  (-> form
      (analyze)
      (run-passes)
      (emit)))

(defmacro compile-template [& body]
  (compile-template* (conj body 'compile-template)))

(comment
  (defmacro compile-template [& body]
    (analyze (conj body 'compile-template)))

  (-> '($ :div
         ($ :span "aaa")
         " "
         ($ :span "bbb"))
      analyze
      run-passes)

  (run-passes
   (analyze
    '($ :<>
       ($ :div "hello"))))

  (macroexpand '(solid.alpha.compiler/compile-template :div "hello"
                                                 (solid.alpha.compiler/compile-template :span "world")))
  (macroexpand '(solid.alpha.core/defc counter []
                  (solid.alpha.compiler/compile-template :div "hello"
                                                   (solid.alpha.compiler/compile-template :span "world"))))

  ; (macroexpand '(compile-template :span {:class cls}))

  #_(macroexpand '(compile-template :button {:onClick #(set-value inc)}
                                    "Value: " value))

  #_(macroexpand-1 '(compile-template counter {:a 1 :b 2}))
  #_(macroexpand-1 '(compile-template :div
                                      (compile-template :span "aaa")
                                      " "
                                      (compile-template :span "bbb"))))
