(ns solid.app
  (:require
   ["solid-js" :as solid :refer [createSignal]]
   ["solid-js/web" :refer [render]]
   [solid.core :as s :refer [defc $]]
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

(defn nested-b1 [{:keys [state]}]
  (let [{:keys [a]} state
        {:keys [b1 b2]} a]
    ($ :div
      ($ :div #(str @b1 " " (js/Date.now)))
      ($ :div #(str @b2 " " (js/Date.now))))))

(defn nested-b2 [props]
  (let [{:keys [b2]} (-> props :state :a)]
    ($ :div #(str @b2 " " (js/Date.now)))))

(defc nested []
  (let [[state set-state] (createSignal {:a {:b1 0
                                             :b2 0}
                                         :c 0})
        {:keys [c] :as state-bean} (s/make-rbean state)]
    ($ :<>
      ($ :div
        ($ :button {:on-click #(set-state (update-in (state) [:a :b1] inc))}
          "B1")
        ($ nested-b1 {:state state-bean}))
      ($ :div
        ($ :button {:on-click #(set-state (update-in (state) [:a :b2] inc))}
          "B2")
        ($ nested-b2 {:state state-bean}))
      ($ :div
        ($ :button {:on-click #(set-state (update-in (state) [:c] inc))}
          "C")
        ($ :div c)
        ($ :div #(str (c) " " (js/Date.now)))))))

(defc app []
  ($ :<>
    ($ nested)))
    ; ($ demo)
    ; ($ todos/main)
    ; ($ tutorial/main)))

(defn ^:dev/after-load start []
  (let [el (.getElementById js/document "app")]
    (set! (.-innerText el) "")
    (render app el)))

(defn ^:export main []
  (start))
