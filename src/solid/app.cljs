(ns solid.app
  (:require ["solid-js" :refer [createEffect For]]
            ["solid-js/store" :refer [createStore]]
            ["solid-js/web" :refer [render]]
            ["solid-js/h" :as h]))

(defn createLocalStore
  [initState]
  (let [[state setState] (createStore initState)]
    (when (.-todos js/localStorage)
      (setState (.parse js/JSON (.-todos js/localStorage))))
    (createEffect
     (fn [] (set! (.-todos js/localStorage) (.stringify js/JSON state))))
    #js [state setState]))

(defn App []
  (let [[state setState] (createLocalStore #js {:todos #js [], :newTitle "", :idCounter 0})]
    #js [(h "h3" "Simple Todos Example")
         (h "input" #js {:type "text",
                         :placeholder "enter todo and click +",
                         :value (fn [] (.-newTitle state)),
                         :onInput (fn [e] (setState "newTitle" (.. e -target -value)))})
         (h "button" #js {:onClick (fn []
                                     (setState (fn [^js s]
                                                 #js {:idCounter (+ (.-idCounter s) 1),
                                                      :todos (.concat
                                                              #js [] (.-todos s)
                                                              #js {:id (.-idCounter state),
                                                                   :title (.-newTitle state),
                                                                   :done false}),
                                                      :newTitle ""})))}
            "+")
         (h For #js {:each (fn [] (.-todos state))}
            (fn [todo]
              (h "div"
                 (h "input"
                    #js {:type "checkbox",
                         :checked (.-done todo),
                         :onChange (fn [e]
                                     (setState "todos" (.findIndex
                                                        (.-todos state)
                                                        (fn [t] (= (.-id t) (.-id todo))))
                                               #js {:done (.. e -target -checked)}))})
                 (h "input" #js {:type "text",
                                 :value (.-title todo),
                                 :onChange (fn [e]
                                             (setState "todos" (.findIndex
                                                                (.-todos state)
                                                                (fn [t] (= (.-id t) (.-id todo))))
                                                       #js {:title (.. e -target -value)}))})
                 (h "button" #js {:onClick (fn []
                                             (setState "todos"
                                                       (fn [t]
                                                         (.filter t (fn [t] (not= (.-id t) (.-id todo)))))))}
                    "x"))))]))

(defn ^:dev/after-load start
  []
  (let [el (.getElementById js/document "app")]
    (set! (.-innerText el) "")
    (render App el)))

(defn ^:export main
  []
  (start))
