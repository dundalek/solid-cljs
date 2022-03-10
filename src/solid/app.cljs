(ns solid.app
  (:require
   ["solid-js" :refer [createSignal]]
   ["solid-js/web" :refer [render]]
   [solid.core :refer [defc $]]
   [solid.todos :as todos]
   [solid.tutorial :as tutorial]))

(defc counter [{:keys [size set-size add-ten]}]
  ($ :div
    ($ :button
      {:onClick #(set-size inc)}
      "Child Add")
    ($ :button
      {:onClick add-ten}
      "Child Add Ten")
    ($ :div size)
    ($ :div #(* (size) 2))))

(defc demo []
  (let [[size set-size] (createSignal 10)]
    ($ :<>
      ($ :button {:onClick #(set-size inc)} "Parent Add")
      ($ counter {:size 100
                  :set-size #()
                  :add-ten #()})
      ($ counter {:size size
                  :set-size set-size
                  :add-ten #(set-size (+ (size) 10))}))))

(defc app []
  ($ :<>
    ($ demo)
    ($ todos/main)
    ($ tutorial/main)))

(defn ^:dev/after-load start []
  (let [el (.getElementById js/document "app")]
    (set! (.-innerText el) "")
    (render app el)))

(defn ^:export main []
  (start))
