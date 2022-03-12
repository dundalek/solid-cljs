(ns todomvc.views
  (:require [re-frame.core :as rf :refer [dispatch]]
            [re-frame.db :refer [app-db]]
            [clojure.string :as str]
            [solid.core :as sl :refer [defc $ $js]]
            ["solid-js" :as s]
            [reagent.ratom :as ratom]))

(defn use-reaction [reaction]
  (let [key (js-obj)
        initial-value (binding [ratom/*ratom-context* (js-obj)]
                        @reaction)
        [sig set-value] (s/createSignal initial-value)]
    (add-watch reaction key (fn [_key _ref _old-value new-value]
                              (set-value new-value)))
    (s/onCleanup (fn []
                   (remove-watch reaction key)))
    sig))

(defn subscribe
  ([query]
   (use-reaction (rf/subscribe query)))
  ([query dynv]
   (use-reaction (rf/subscribe query dynv))))

(defc todo-input [{:keys [id class placeholder title on-save on-stop]}]
  (let [[value set-value] (s/createSignal @title)
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
    ($ :li {:class #(str (when @done "completed ")
                         (when (editing) "editing"))}
      ($ :div.view
        ($ :input.toggle
          {:type "checkbox"
           :checked done
           :on-change #(dispatch [:toggle-done @id])})
        ($ :label
          {:on-dblclick #(set-editing true)}
          title)
        ($ :button.destroy
          {:on-click #(dispatch [:delete-todo @id])}))
      (sl/when editing
        ($ todo-input
          {:class "edit"
           :title #(title)
           :on-save (fn [text]
                      (js/console.log "on-save" text)
                      (if (seq text)
                        (dispatch [:save @id text])
                        (dispatch [:delete-todo @id])))
           :on-stop #(set-editing false)})))))

(defc task-list
  []
  (let [visible-todos (subscribe [:visible-todos])
        all-complete? (subscribe [:all-complete?])]
    ($ :section#main
      ($ :input#toggle-all
        {:type "checkbox"
         :checked all-complete?
         :on-change #(dispatch [:complete-all-toggle])})
      ($ :label
        {:for "toggle-all"}
        "Mark all as complete")
      ($ :ul#todo-list
        (sl/for [todo #(to-array (visible-todos))]
          ($ todo-item todo))))))

(defc footer-controls
  []
  (let [;; TODO support reactive desctructuring
        ;[active done] (subscribe [:footer-counts])
        footer-counts (subscribe [:footer-counts])
        active (s/createMemo #(first (footer-counts)))
        done (s/createMemo #(second (footer-counts)))
        showing       (subscribe [:showing])
        a-fn          (fn [filter-kw txt]
                        ($ :a {:class #(when (= filter-kw (showing)) "selected")
                               :on-click #(dispatch [:set-showing filter-kw])}
                          txt))]
    ($ :footer#footer
      ($ :span#todo-count
        ($ :strong active) " " #(case (active) 1 "item" "items") " left")
      ($ :ul#filters
        ($ :li (a-fn :all    "All"))
        ($ :li (a-fn :active "Active"))
        ($ :li (a-fn :done   "Completed")))
      (sl/when #(pos? (done))
        ($ :button#clear-completed {:on-click #(dispatch [:clear-completed])}
          "Clear completed")))))

(defc task-entry
  []
  ($ :header#header
    ($ :h1 "todos")
    ($ todo-input
      {:id "new-todo"
       :placeholder "What needs to be done?"
       :on-save #(when-not (str/blank? %)
                   (dispatch [:add-todo %]))})))

(defc todo-app
  []
  (let [todos (subscribe [:todos])]
    ($ :<>
      ($ :section#todoapp
        ($ task-entry)
        (sl/when #(seq (todos))
          ($ task-list))
        ($ footer-controls))
      ($ :footer#info
        ($ :p "Double-click to edit a todo")))))
