(ns solid.core
  (:require ["solid-js/h" :as hyperscript]
            [cljs-bean.core]
            [goog.object :as gobj]
            ["solid-js" :as s])
  (:require-macros [solid.core]))

#_(def proxy-props-handler
    #js {:get (fn [target prop]
                (fn [& args]
                  (let [x (gobj/get target prop)]
                    (if (fn? x)
                      (apply x args)
                      x))))})

(declare make-rbean)

(deftype RProps [m]
  ILookup
  (-lookup [_ k]
    (make-rbean
     (get m k))))

(deftype RBean [f]
  ILookup
  (-lookup [_ k]
    (make-rbean
     (s/createMemo
      (fn [] (get (f) k)))))
  #_(-lookup [_ k not-found])

  IFn
  (-invoke [_]
    (f))
  (-invoke [_ a]
    (f a))

  IDeref
  (-deref [_]
    (f)))

(defn make-rprops [m]
  (if (instance? RBean m)
    m
    (->RProps m)))

(defn make-rbean [x]
  (cond
    (instance? RBean x) x
    (not (fn? x)) (->RBean (fn [] x))
    (zero? (.-length x)) (->RBean x)
    :else x))

(defn wrap-rbean [x]
  ;; Wrapping so that solid does not treat it as plain object insted of callable
  (if (instance? RBean x)
    (fn [] (x))
    x))

(defn h [& args]
  (->> args
       (map wrap-rbean)
       (apply hyperscript)))

(def For s/For)
(def Show s/Show)
