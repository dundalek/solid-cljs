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
