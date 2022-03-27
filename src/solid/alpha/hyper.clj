(ns solid.alpha.hyper
  (:require [solid.alpha.impl.core :as core]))

(defmacro defc [& body]
  `(defn ~@body))

(defmacro $ [el & body]
  (cond
    (= el :<>) `(cljs.core/array ~@body)
    (string? el) `(solid.alpha.hyper/h ~el ~@(core/with-js-props body))
    (keyword? el) `(solid.alpha.hyper/h ~(name el) ~@(core/with-js-props body))
    (symbol? el) `(solid.alpha.hyper/h ~el ~@(core/with-clj-props body))
    :else (throw (ex-info (str "Expected keyword, string or symbol as element, received: " el)
                          {:el el}))))

(defmacro $js [el & body]
  `(solid.alpha.hyper/h ~el ~@(core/with-js-props body)))
