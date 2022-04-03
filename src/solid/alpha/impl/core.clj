(ns solid.alpha.impl.core
  (:require [camel-snake-kebab.core :as csk]
            [clojure.string :as str]
            [solid.alpha.compiler :as compiler]))

(defn wrap-expression [v]
  (if (compiler/primitive? v)
    v
    `(fn [] ~v)))

(defn wrap-expression-args [children]
  (map wrap-expression children))

(defn with-js-props [body]
  (let [[props & other] body]
    (if (map? props)
      (cons
       (cons 'js-obj (mapcat
                      (fn [[k v]]
                        (let [k (if (keyword? k) (name k) (str k))]
                          [(if (str/starts-with? k "on-")
                             (->> (subs k 3)
                                  ;; maybe upper first character will be enough
                                  ;; it is a bit dumb, since hyperscript wrapper will convert it back to lower case to pass to DOM
                                  (csk/->PascalCaseString)
                                  (str "on"))
                             ;; keep attributes like aria-hidden
                             k)
                           `(solid.alpha.core/wrap-rbean ~v)]))
                      props))
       (wrap-expression-args other))
      (wrap-expression-args body))))

(defn with-clj-props [body]
  (let [[props & other] body]
    (if (map? props)
      (cons
       `(solid.alpha.core/make-rprops ~props)
       other)
      body)))

(defn flow-for [[item items fallback-kw fallback] & body]
  `(solid.alpha.core/$js solid.alpha.core/For (cljs.core/js-obj "each" ~items
                                                                ~@(when (= fallback-kw :fallback)
                                                                    ["fallback" fallback]))

     (fn [~item]
       ~@body)))

(defn flow-if
  ([test then]
   `(solid.alpha.core/$js solid.alpha.core/Show (cljs.core/js-obj "when" ~test)
      (fn []
        ~then)))
  ([test then else]
   `(solid.alpha.core/$js solid.alpha.core/Show (cljs.core/js-obj "when" ~test
                                                                  "fallback" ~else)

      (fn []
        ~then))))

(defn flow-when [test & body]
  `(solid.alpha.core/$js solid.alpha.core/Show (cljs.core/js-obj "when" ~test)
     (fn []
       ~@body)))
