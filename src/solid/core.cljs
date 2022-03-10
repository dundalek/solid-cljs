(ns solid.core
  (:require ["solid-js/h" :as hyperscript]
            [cljs-bean.core]
            [goog.object :as gobj])
  (:require-macros [solid.core]))

(def h hyperscript)

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
               (assoc m k (if (fn? x)
                            x
                            (fn [] x))))
             {}
             props))
