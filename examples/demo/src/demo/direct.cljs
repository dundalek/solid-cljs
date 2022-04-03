(ns demo.direct
  (:require
   ["solid-js" :as solid]
   ["solid-js/web" :refer [template insert]]))

(def _tmpl$ (template "<div>Count value is </div>" 2))

(defn CountingComponent []
  (let [[count setCount] (solid/createSignal 0)
        interval (js/setInterval (fn []
                                   (setCount (fn [c] (+ c 1))))
                                 1000)]
    (solid/onCleanup (fn [] (js/clearInterval interval)))
    ((fn []
       (let [_el$ (.cloneNode _tmpl$ true)]
         (insert _el$ count nil)
         _el$)))))
