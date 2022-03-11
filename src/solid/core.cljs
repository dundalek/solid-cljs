(ns solid.core
  (:require ["solid-js/h" :as hyperscript]
            [cljs-bean.core]
            [goog.object :as gobj])
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

(def proxy-props-handler
  #js {:get (fn [target prop]
              (fn [& args]
                (let [x (gobj/get target prop)]
                  (if (fn? x)
                    (apply x args)
                    x))))})

;; TODO make this lazy or via protocol
(defn make-callable-props [props]
  (reduce-kv (fn [m k x]
               (assoc m k (cond
                            (not (fn? x)) (->RProp (fn [] x))
                            (zero? (.-length x)) (->RProp x)
                            :else x)))
             {}
             props))
