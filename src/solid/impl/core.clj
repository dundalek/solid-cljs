(ns solid.impl.core
  (:require [camel-snake-kebab.core :as csk]
            [clojure.string :as str]))

(defn defui1 [& body]
  `(defn ~@body))

(defn $1 [el & body]
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

#_(defn $ [el & body]
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
#_(defn defc [fn-name params & body]
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
                        (let [k (if (keyword k) (name k) (str k))]
                          [#_(csk/->camelCaseString k)
                           (if (str/starts-with? "on-" k)
                             (csk/->camelCaseString k)
                             ;; keep attributes like aria-hidden
                             k)
                           v
                           `(solid.core/wrap-rbean ~v)]))
                      props))
       other)
      body)))

(defn $ [el & body]
  (cond
    (= el :<>) `(cljs.core/array ~@body)
    (string? el) `(solid.core/h ~el ~@(with-js-props body))
    (keyword? el) `(solid.core/h ~(name el) ~@(with-js-props body))
    (symbol? el) `(solid.core/h ~el ~@body)
    :else (throw (ex-info (str "Expected keyword or symbol as element, received: " el)
                          {:el el}))))

(defn $js [el & body]
  (cond
    (symbol? el) `(solid.core/h ~el ~@(with-js-props body))
    :else (throw (ex-info (str "Expected symbol as element, received: " el)
                          {:el el}))))
;; TODO: needs to be smarter to handle docstrings, annotations, etc.
(defn defc [fn-name params & body]
  (if (seq params)
    #_`(defn ~fn-name params
           ~@body)
    `(defn ~fn-name [props#]
       (let [~(first params) (solid.core/make-rprops props#)]
         ~@body))
    `(defn ~fn-name []
       ~@body)))

(defn flow-for [[item items fallback-kw fallback] & body]
  `(solid.core/$js solid.core/For (cljs.core/js-obj "each" ~items
                                                    ~@(when (= fallback-kw :fallback)
                                                        ["fallback" fallback]))

     (fn [~item]
       ~@body)))

(defn flow-if
  ([test then]
   `(solid.core/$js solid.core/Show (cljs.core/js-obj "when" ~test)
      (fn []
        ~then)))
  ([test then else]
   `(solid.core/$js solid.core/Show (cljs.core/js-obj "when" ~test
                                                      "fallback" ~else)

      (fn []
        ~then))))

(defn flow-when [test & body]
  `(solid.core/$js solid.core/Show (cljs.core/js-obj "when" ~test)
     (fn []
       ~@body)))
