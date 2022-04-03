(ns todomvc.views
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf :refer [dispatch]]
   [reagent.ratom :as ratom]
   [solid.alpha.core :as sl]
   [solid.alpha.hyper :refer [defc $]]))

(defn use-reaction [reaction]
  (let [key (js-obj)
        initial-value (binding [ratom/*ratom-context* (js-obj)]
                        @reaction)
        [sig set-value] (sl/create-signal initial-value)]
    (add-watch reaction key (fn [_key _ref _old-value new-value]
                              (set-value new-value)))
    (sl/on-cleanup (fn []
                     (remove-watch reaction key)))
    sig))

(defn subscribe
  ([query]
   (use-reaction (rf/subscribe query)))
  ([query dynv]
   (use-reaction (rf/subscribe query dynv))))

(defc todo-input [{:keys [class placeholder title on-save on-stop]}]
  (let [[value set-value] (sl/create-signal @title)
        stop (fn []
               (set-value "")
               (when on-stop (on-stop)))
        save (fn []
               (on-save (-> (value) str str/trim))
               (stop))]
    ($ :input
      {:class class
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
  (let [[editing set-editing] (sl/create-signal false)]
    ($ :li {:class #(str (when @done "completed ")
                         (when (editing) "editing"))}
      ($ :div.view
        ($ :input.toggle
          {:type "checkbox"
           :checked done
           :on-change #(dispatch [:toggle-done @id])})
        ($ :label
          {:on-dblclick #(set-editing true)}
          @title)
        ($ :button.destroy
          {:on-click #(dispatch [:delete-todo @id])}))
      (sl/when editing
        ($ todo-input
          {:class "edit"
           :title title
           :on-save (fn [text]
                      (js/console.log "on-save" text)
                      (if (seq text)
                        (dispatch [:save @id text])
                        (dispatch [:delete-todo @id])))
           :on-stop #(set-editing false)})))))

(defc task-list []
  (let [visible-todos (subscribe [:visible-todos])
        all-complete? (subscribe [:all-complete?])]
    ($ :section.main
      ($ :input#toggle-all.toggle-all
        {:type "checkbox"
         :checked all-complete?
         :on-change #(dispatch [:complete-all-toggle])})
      ($ :label
        {:for "toggle-all"}
        "Mark all as complete")
      ($ :ul.todo-list
        (sl/for [todo (to-array (visible-todos))]
          ($ todo-item (sl/make-rprops todo)))))))

(defc footer-controls []
  (let [;; TODO support reactive desctructuring
        ;[active done] (subscribe [:footer-counts])
        footer-counts (subscribe [:footer-counts])
        active (sl/create-memo #(first (footer-counts)))
        done (sl/create-memo #(second (footer-counts)))
        showing       (subscribe [:showing])
        a-fn          (fn [filter-kw txt]
                        ($ :a {:class #(when (= filter-kw (showing)) "selected")
                               :on-click #(dispatch [:set-showing filter-kw])}
                          txt))]
    ($ :footer.footer
      ($ :span.todo-count
        ($ :strong active) " " (case (active) 1 "item" "items") " left")
      ($ :ul.filters
        ($ :li (a-fn :all    "All"))
        ($ :li (a-fn :active "Active"))
        ($ :li (a-fn :done   "Completed")))
      (sl/when #(pos? (done))
        ($ :button.clear-completed {:on-click #(dispatch [:clear-completed])}
          "Clear completed")))))

(defc task-entry []
  ($ :header.header
    ($ :h1 "todos")
    ($ todo-input
      {:class "new-todo"
       :placeholder "What needs to be done?"
       :on-save #(when-not (str/blank? %)
                   (dispatch [:add-todo %]))})))

(defc todo-app []
  (let [todos (subscribe [:todos])]
    ($ :<>
      ($ :section.todoapp
        ($ task-entry)
        (sl/when #(seq (todos))
          ($ task-list))
        ($ footer-controls))
      ($ :footer.info
        ($ :p "Double-click to edit a todo")))))
