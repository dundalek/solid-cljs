(ns solid.core)

(defmacro defui1 [& body]
  `(defn ~@body))

(defmacro $1 [el & body]
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

#_(defmacro $ [el & body]
    (let [[props & body] body
          body (cons
                (if (map? props)
                  (cons 'js-obj
                        (mapcat
                         (fn [[k v]]
                           [(if (keyword? k) (name k) k)
                            v])
                         props))
                  props)
                body)]
      (cond
        (= el :<>) `(cljs.core/array ~@body)
        (string? el) `(solid.core/h ~el ~@body)
        (keyword? el) `(solid.core/h ~(name el) ~@body)
        (symbol? el) `(solid.core/h ~el ~@body)
        :else (throw (ex-info (str "Expected keyword or symbol as element, received: " el)
                              {:el el})))))

;; TODO: needs to be smarter to handle docstrings, annotations, etc.
#_(defmacro defc [fn-name params & body]
    (if (seq params)
      `(defn ~fn-name [props#]
         (let [~(first params) (cljs-bean.core/bean
                                (js/Proxy. props# solid.core/proxy-props-handler))]

           ~@body))
      `(defn ~fn-name []
         ~@body)))

(defn- with-js-props [body]
  (let [[props & other] body]
    (if (map? props)
      (cons
       (cons 'js-obj (mapcat
                      (fn [[k v]]
                        [(if (keyword? k) (name k) k)
                         v])
                      props))
       other)
      body)))

(defmacro $ [el & body]
  (cond
    (= el :<>) `(cljs.core/array ~@body)
    (string? el) `(solid.core/h ~el ~@(with-js-props body))
    (keyword? el) `(solid.core/h ~(name el) ~@(with-js-props body))
    (symbol? el) `(solid.core/h ~el ~@body)
    :else (throw (ex-info (str "Expected keyword or symbol as element, received: " el)
                          {:el el}))))

(defmacro $js [el & body]
  (cond
    (symbol? el) `(solid.core/h ~el ~@(with-js-props body))
    :else (throw (ex-info (str "Expected symbol as element, received: " el)
                          {:el el}))))
;; TODO: needs to be smarter to handle docstrings, annotations, etc.
(defmacro defc [fn-name params & body]
  (if (seq params)
    `(defn ~fn-name [props#]
       (let [~(first params) (solid.core/make-callable-props props#)]
         ~@body))
    `(defn ~fn-name []
       ~@body)))
