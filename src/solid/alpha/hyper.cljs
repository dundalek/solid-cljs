(ns solid.alpha.hyper
  (:require
   ["solid-js/h" :as hyperscript]
   [goog.object :as gobj]
   solid.alpha.core)
  (:require-macros [solid.alpha.hyper]))

#_(def proxy-props-handler
    #js {:get (fn [target prop]
                (fn [& args]
                  (let [x (gobj/get target prop)]
                    (if (fn? x)
                      (apply x args)
                      x))))})

(def h hyperscript)
