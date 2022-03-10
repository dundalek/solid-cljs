(ns todomvc.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [solid.core :refer [defc $ $js]]
            ["solid-js" :as s]
            [camel-snake-kebab.core]))

(defc todo-input [{:keys [id class placeholder title on-save on-stop]}]
  (let [[value set-value] (s/createSignal (title))
        stop (fn []
               (set-value "")
               (when on-stop (on-stop)))
        save (fn []
               (on-save (-> (value) str str/trim))
               (stop))]
    ($ :input
      {:id id
       :class class
       :type        "text"
       :placeholder placeholder
       :value       value
       :auto-focus  true
       :on-blur     save
       :on-input    #(set-value (.. % -target -value))
       :on-keydown (fn [ev]
                     (case (.-which ev)
                       13 (save)
                       27 (stop)
                       nil))})))

(defc todo-item [{:keys [id done title]}]
  (let [[editing set-editing] (s/createSignal false)]
    ($ :li {:class #(str (when (done) "completed ")
                         (when (editing) "editing"))}
      ($ :div.view
        ($ :input.toggle
          {:type "checkbox"
           :checked done
           :on-change #(dispatch [:toggle-done (id)])})
        ($ :label
          {:on-dblclick #(set-editing true)}
          title)
        ($ :button.destroy
          {:on-click #(dispatch [:delete-todo (id)])}))
      ($js s/Show {:when editing}
        (fn []
          ($ todo-input
            {:class "edit"
             :title title
             :on-save #(if (seq %)
                         (dispatch [:save (id) %])
                         (dispatch [:delete-todo (id)]))
             :on-stop #(set-editing false)}))))))

(defc task-list
  []
  (let [visible-todos @(subscribe [:visible-todos])
        all-complete? @(subscribe [:all-complete?])]
    ($ :section#main
      ($ :input#toggle-all
        {:type "checkbox"
         :checked all-complete?
         :on-change #(dispatch [:complete-all-toggle])})
      ($ :label
        {:for "toggle-all"}
        "Mark all as complete")
      ($ :ul#todo-list
        ($js s/For {:each (to-array visible-todos)}
          (fn [todo]
            ($ todo-item todo)))))))

(defc footer-controls
  []
  (let [[active done] @(subscribe [:footer-counts])
        showing       @(subscribe [:showing])
        a-fn          (fn [filter-kw txt]
                        ($ :a {:class (when (= filter-kw showing) "selected")
                               :on-click #(dispatch [:set-showing filter-kw])}
                          txt))]
    ($ :footer#footer
      ($ :span#todo-count
        ($ :strong active) " " (case active 1 "item" "items") " left")
      ($ :ul#filters
        ($ :li (a-fn :all    "All"))
        ($ :li (a-fn :active "Active"))
        ($ :li (a-fn :done   "Completed")))
      (when (pos? done)
        ($ :button#clear-completed {:on-click #(dispatch [:clear-completed])}
          "Clear completed")))))

(defc task-entry
  []
  ($ :header#header
    ($ :h1 "todos")
    ($ todo-input
      {:id "new-todo"
       :placeholder "What needs to be done?"
       ;; TODO: remove after handling missing props in callable props
       :title ""
       :on-save #(when-not (str/blank? %)
                   (dispatch [:add-todo %]))})))

(defc todo-app
  []
  ($ :<>
    ($ :section#todoapp
      ($ task-entry)
      ($js s/Show {:when (seq @(subscribe [:todos]))}
        (fn []
          ($ task-list)))
      ($ footer-controls))
    ($ :footer#info
      ($ :p "Double-click to edit a todo"))))
