(ns demo.todos
  (:require ["solid-js" :refer [createEffect For]]
            ["solid-js/store" :refer [createStore]]
            [solid.alpha.hyper :refer [defc $ $js]]))

(defn createLocalStore
  [initState]
  (let [[state setState] (createStore initState)]
    (when (.-todos js/localStorage)
      (setState (.parse js/JSON (.-todos js/localStorage))))
    (createEffect
     (fn [] (set! (.-todos js/localStorage) (.stringify js/JSON state))))
    [state setState]))

(defc main []
  (let [[state setState] (createLocalStore #js {:todos #js [] :newTitle "" :idCounter 0})]
    ($ :<>
      ($ :h3 "Simple Todos Example")
      ($ :input {:type "text"
                 :placeholder "enter todo and click +"
                 :value (fn [] (.-newTitle state))
                 :onInput (fn [e] (setState "newTitle" (.. e -target -value)))})
      ($ :button {:onClick (fn []
                             (setState (fn [^js s]
                                         #js {:idCounter (+ (.-idCounter s) 1)
                                              :todos (.concat
                                                      (.-todos s)
                                                      #js {:id (.-idCounter state)
                                                           :title (.-newTitle state)
                                                           :done false})
                                              :newTitle ""})))}
        "+")
      ($js For {:each (fn [] (.-todos state))}
        (fn [todo i]
          ($ :div
            ($ :input {:type "checkbox"
                       :checked (.-done todo)
                       :onChange (fn [e]
                                   (setState "todos" (i) #js {:done (.. e -target -checked)}))})
            ($ :input {:type "text"
                       :value (.-title todo)
                       :onChange (fn [e]
                                   (setState "todos" (i) #js {:title (.. e -target -value)}))})
            ($ :button {:onClick (fn []
                                   (setState "todos"
                                             (fn [t]
                                               (.filter t (fn [t] (not= (.-id t) (.-id todo)))))))}
              "x")))))))
