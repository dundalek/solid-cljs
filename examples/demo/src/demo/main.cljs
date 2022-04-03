(ns demo.main
  (:require
   ; [demo.todos :as todos]
   [demo.direct :as direct]
   [demo.tutorial :as tutorial]
   [solid.alpha.core :as sc]
   [solid.alpha.hyper :refer [defc $]]
   [solid.alpha.web :as web])) ;:refer [defc $]]

(defc counter [{:keys [size set-size add-ten]}]
  ($ :div
    ($ :button
      {:on-click #(set-size inc)}
      "Child Add")
    ($ :button
      {:on-click #(add-ten)}
      "Child Add Ten")
    ($ :div #(size))
    ($ :div #(* (size) 2))))

(defc demo []
  (let [[size set-size] (sc/create-signal 10)]
    ($ :<>
      ($ :button {:on-click #(set-size inc)} "Parent Add")
      ($ counter {:size size
                  :set-size set-size
                  :add-ten #(set-size (+ (size) 10))})
      ($ :div "Static value:")
      ($ counter {:size 100
                  :set-size #()
                  :add-ten #()}))))

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
  (let [[state set-state] (sc/create-signal {:a {:b1 0
                                                 :b2 0}
                                             :c 0})
        {:keys [c] :as state-bean} (sc/make-rbean state)]
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
        ($ :div #(c))
        ($ :div #(str (c) " " (js/Date.now)))))))

(defn build-data []
  (->> (range 10)
       (mapv (fn [id]
               {:id id
                :label (str "x" id)}))))

(defc colls []
  (let [[items set-items] (sc/create-signal (build-data))]
    ($ :div
      ($ :button {:on-click (fn []
                              (set-items (update-in (items) [1 :label] #(str % " !!"))))}
        "Update")
      ($ :button {:on-click (fn []
                              (let [items (items)]
                                (set-items (-> items
                                               (assoc 1 (get items 8))
                                               (assoc 8 (get items 1))))))}
        "Swap")
      ($ :button {:on-click #(set-items (build-data))}
        "Replace")
      (sc/reactive-for #(into-array (items))
                       (fn [item]
                         (let [id (sc/create-memo #(:id (item)))
                               label (sc/create-memo #(:label (item)))]
                           ($ :div {:on-click (fn []
                                                (let [item (item)]
                                                  (set-items
                                                   (into [] (remove #(identical? item %))
                                                         (items)))))}
                             ($ :div
                               ($ :span id)
                               " "
                               ($ :span label))))))
      #_(sc/for [item #(into-array (items))]
          (let [id (:id item)
                label (:label item)]
            ($ :div
              ($ :span id)
              " "
              ($ :span label)))))))

(defc simple []
  (let [[value set-value] (sc/create-signal 0)]
    ($ :button {:on-click #(set-value inc)}
      "Value: " value)))

(defc app []
  ($ :div
    ($ :h1 "Solid CLJS demo")
    ($ :div "Hello World!")

    ($ :h2 "Simple counter")
    ($ simple)

    ($ :h2 "Direct using Solid primitives")
    ($ direct/CountingComponent)

    ($ :h2 "demo")
    ($ demo)

    ($ :h2 "nested")
    ($ nested)

    ($ :h2 "colls")
    ($ colls)

    ; ($ todos/main)
    ($ :h2 "tutorial")
    ($ tutorial/main)))

(defn ^:dev/after-load start []
  (let [el (.getElementById js/document "app")]
    (set! (.-innerText el) "")
    (web/render app el)))

(defn ^:export main []
  (start))
