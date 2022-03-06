(ns solid.app
  (:require
   ["solid-js/web" :refer [render]]
   [solid.core :refer [defui $]]
   [solid.todos :as todos]
   [solid.tutorial :as tutorial]))

(defui app []
  ($ :<>
    ($ todos/main)
    ($ tutorial/main)))

(defn ^:dev/after-load start []
  (let [el (.getElementById js/document "app")]
    (set! (.-innerText el) "")
    (render app el)))

(defn ^:export main []
  (start))
