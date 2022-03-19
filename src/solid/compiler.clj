(ns solid.compiler
  (:require [clojure.string :as str]))

(defn escape [s]
  ;; TODO use proper escaping, see dom-expressions/server.js escape
  (str/replace s "<" "&lt;"))

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

(defn compile-tag [el children]
  (cond (keyword? el)
        (let [tag (name el)]
          (str "<" tag ">"
               children
               "</" tag ">"))))

(defmacro compile-static [el & body]
  (compile-tag el (compile-children body)))

(defmacro compile-template [el & body]
  (let [tmpl-sym (gensym "tmpl")
        el-sym (gensym "el")
        child-symbols (mapv (fn [_] (gensym "el")) body)
        [_ child-bindings] (reduce
                            (fn [[sibling-sym bindings] sym]
                              [sym (-> bindings
                                       (conj sym)
                                       (conj `(some-> ~sibling-sym .-nextSibling)))])
                            [(first child-symbols)
                             [(first child-symbols) `(.-firstChild ~el-sym)]]
                            (rest child-symbols))
        bindings (into [tmpl-sym `(solid.web/template ~(compile-tag el (compile-children body)))
                        el-sym `(.cloneNode ~tmpl-sym true)]
                       child-bindings)
        ops (->> (map list body child-symbols)
                 (keep (fn [[expr sym]]
                         (when-not (primitive? expr)
                           `(solid.web/insert ~el-sym ~expr ~sym)))))]
    (list `(fn []
             (let ~bindings
               ~@ops
               ~el-sym)))))

(comment
  (macroexpand-1 '(compile-template :span " " greeting " " name " ")))

