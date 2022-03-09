(ns solid.core)

(defmacro defui [& body]
  `(defn ~@body))

(defmacro $ [el & body]
  (let [[props & body] body
        body (cons
              (if (map? props) `(cljs.core/clj->js ~props) props)
              body)]
    (cond
      (= el :<>) `(cljs.core/array ~@body)
      (string? el) `(solid.core/h ~el ~@body)
      (keyword? el) `(solid.core/h ~(name el) ~@body)
      (symbol? el) `(solid.core/h ~el ~@body)
      :else (throw (ex-info (str "Expected keyword or symbol as element, received: " el)
                            {:el el})))))

;; TODO: needs to be smarter to handle docstrings, annotations, etc.
(defmacro defui2 [fn-name [props-binding] & body]
  `(defn ~fn-name [props#]
     (let [~props-binding (cljs-bean.core/bean
                           (js/Proxy. props# solid.core/proxy-props-handler))]

       ~@body)))
