(ns solid.compiler
  (:require [clojure.string :as str]))

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
                                   `(solid.web/effect
                                     (~'fn [] ~assig-expr))))

                  (str/starts-with? k-str "on")
                  (let [event-name (-> k-str
                                       (str/replace #"^on-?" "")
                                       (str/lower-case))]
                    `(.addEventListener ~el-sym ~event-name ~v))

                  :else `(goog.object/set ~el-sym ~k-str ~v)))))))

(defmacro compile-static [el & body]
  (compile-tag el nil (compile-children body)))

(do
  (defmacro compile-template [el & body]
    (cond
      (= el :<>) `(cljs.core/array ~@body)
      (symbol? el) `(solid.web/create-component ~el ~@body)
      (keyword? el)
      (let [tmpl-sym (gensym "tmpl")
            el-sym (gensym "el")
            props (when (map? (first body))
                    (first body))
            props-divided (->> props
                               (group-by (fn [[_k v]]
                                           (primitive? v))))
            static-props (get props-divided true)
            dynamic-props (get props-divided false)
            prop-ops (props->ops el-sym dynamic-props)
            children (if props
                       (rest body)
                       body)
            child-symbols (mapv (fn [_] (gensym "el")) children)
            child-bindings (when (seq children)
                             (second
                              (reduce
                               (fn [[sibling-sym bindings] sym]
                                 [sym (-> bindings
                                          (conj sym)
                                          (conj `(some-> ~sibling-sym .-nextSibling)))])
                               [(first child-symbols)
                                [(first child-symbols) `(.-firstChild ~el-sym)]]
                               (rest child-symbols))))
            bindings (into [tmpl-sym `(solid.web/template ~(compile-tag el static-props (compile-children children)))
                            el-sym `(.cloneNode ~tmpl-sym true)]
                           child-bindings)
            ops (->> (map list children child-symbols)
                     (keep (fn [[expr sym]]
                             (when-not (primitive? expr)
                               `(solid.web/insert ~el-sym ~expr ~sym)))))]
        (list `(fn []
                 (let ~bindings
                   ~@prop-ops
                   ~@ops
                   ~el-sym))))))

  ; (macroexpand '(compile-template :span {:class cls}))

  (macroexpand '(compile-template :button {:onClick #(set-value inc)}
                                  "Value: " value))

  #_(macroexpand-1 '(compile-template counter {:a 1 :b 2}))
  #_(macroexpand-1 '(compile-template :div
                                      (compile-template :span "aaa")
                                      " "
                                      (compile-template :span "bbb"))))

