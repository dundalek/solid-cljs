(ns solid.core
  (:require ["solid-js/h" :as hyperscript]
            [cljs-bean.core]
            [goog.object :as gobj]
            ["solid-js" :as s])
  (:require-macros [solid.core]))

(deftype RProp [f]
  IFn
  (-invoke [_]
    (f))

  IDeref
  (-deref [_]
    (f)))

(defn wrap-rprop [x]
  ;; Wrapping so that solid does not treat it as plain object insted of callable
  (if (instance? RProp x)
    (fn [] (x))
    x))

(defn h [& args]
  (->> args
       (map wrap-rprop)
       (apply hyperscript)))

#_(def proxy-props-handler
    #js {:get (fn [target prop]
                (fn [& args]
                  (let [x (gobj/get target prop)]
                    (if (fn? x)
                      (apply x args)
                      x))))})

(deftype RBean [m]
  ILookup
  (-lookup [_ k]
    (let [x (get m k)]
      (cond
        (instance? RProp x) x
        (not (fn? x)) (->RProp (fn [] x))
        (zero? (.-length x)) (->RProp x)
        :else x)))
  #_(-lookup [_ k not-found]))

(defn make-callable-props [props]
  (->RBean props))

(def For s/For)
(def Show s/Show)
