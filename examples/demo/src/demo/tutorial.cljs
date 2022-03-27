(ns demo.tutorial
  (:require
   [clojure.string :as str]
   ["solid-js" :refer [createSignal createEffect onCleanup onMount mergeProps
                       Show For Switch Match Dynamic Portal ErrorBoundary]]
   ["solid-js/web" :refer [template render createComponent insert]]
   ["solid-js/store" :refer [createStore]]
   [solid.alpha.core :refer [defui1 $1] :rename {defui1 defui $1 $}]))

(defui CountingComponent []
  (let [[count setCount] (createSignal 0)
        interval (js/setInterval #(setCount (fn [c] (+ c 1))) 1000)]
    (onCleanup #(js/clearInterval interval))
    ($ :div "Count value is " count)))

(comment
  (def _tmpl$ (template "<div>Count value is </div>" 2))

  (defn CountingComponent []
    (let [[count setCount] (createSignal 0)
          interval (js/setInterval (fn []
                                     (setCount (fn [c] (+ c 1))))
                                   1000)]
      (onCleanup (fn [] (js/clearInterval interval)))
      ((fn []
         (let [_el$ (.cloneNode _tmpl$ true)
               _ (.-firstChild _el$)]
           (insert _el$ count nil)
           _el$))))))

;; Tutorial

(defui hello-world []
  ($ :div "Hello Solid World!"))

(defui hello-world-svg []
  (let [name "Solid"
        svg ($ :svg {:height "300" :width "400"}
              ($ :defs
                ($ :linearGradient {:id "gr1" :x1 "0%" :y1 "60%" :x2 "100%" :y2 "0%"}
                  ($ :stop {:offset "5%" :style "stop-color:rgb(255,255,3);stop-opacity:1"})
                  ($ :stop {:offset "100%" :style "stop-color:rgb(255,0,0);stop-opacity:1"})))
              ($ :ellipse {:cx "125" :cy "150" :rx "100" :ry "60" :fill "url(#gr1)"})
              "Sorry but this browser does not support inline SVG.")]
    ($ :<>
      ($ :div "Hello " name "!")
      svg)))

(defui counter []
  (let [[count setCount] (createSignal 0)
        double-count #(* (count) 2)]
    (createEffect #(js/console.log "The count is now" (count)))
    ($ :div
      ($ :button {:onClick #(setCount (inc (count)))}
        "Click Me")
      ($ :div
        "Double: " double-count))))

(defui control-flow-show []
  (let [[logged-in set-logged-in] (createSignal false)
        toggle #(set-logged-in (not (logged-in)))]
    ($ Show {:when logged-in
             :fallback ($ :button {:onClick toggle} "Log in")}
      ($ :button {:onClick toggle} "Log out"))))

(defui control-flow-for []
  (let [[cats _set-cats] (createSignal
                          #js [{:id "J---aiyznGQ" :name "Keyboard Cat"}
                               {:id "z_AbfPXTKms" :name "Maru"}
                               {:id "OUtn3pvWmpg" :name "Henri The Existential Cat"}])]
    ($ :ul
      ($ For {:each cats}
        (fn [{:keys [name id]} i]
          ($ :li
            ($ :a {:target "_blank" :href (str "https://www.youtube.com/watch?v=" id)}
              (inc (i)) ": " name)))))))

(defui control-flow-switch []
  (let [[nums] (createSignal #js [3 7 11])
        [x set-x] (createSignal 11)]
    ($ :div
      ($ For {:each nums}
        (fn [num]
          ($ :button {:onClick #(set-x num)} num)))
      ($ Switch {:fallback ($ :p x " is between 5 and 10")}
        ($ Match {:when #(> (x) 10)}
          ($ :p x " is greater than 10"))
        ($ Match {:when #(> 5 (x))}
          ($ :p x " is less than 5"))))))

(defui red-thing []
  ($ :strong {:style "color: red"} "Red Thing"))

(defui green-thing []
  ($ :strong {:style "color: green"} "Green Thing"))

(defui blue-thing []
  ($ :strong {:style "color: blue"} "Blue Thing"))

(def options
  {"red" red-thing
   "green" green-thing
   "blue" blue-thing})

;; TODO
(defui control-flow-dynamic []
  (let [[selected set-selected] (createSignal "red")]
    ($ :<>
      ($ :select {:value selected
                  :onInput (fn [^js e] (set-selected (.-currentTarget.value e)))}
        ($ For {:each (to-array (keys options))}
          (fn [color]
            ($ :option {:value color} color))))
       ;; Dynamic does not seem to work when backed with hyperscript
      #_($ Dynamic {:component (fn [] (options (selected)))})
      ($ Switch {:fallback ($ blue-thing)}
        ($ Match {:when #(= (selected) "red")} ($ red-thing))
        ($ Match {:when #(= (selected) "green")} ($ green-thing))))))

;; TODO
(defui control-flow-portal []
  #_($ :div {:class "app-container"}
      ($ :p "Just some text inside a div that has a restricted size.")
      ($ Portal
        (fn []
          ($ :div {:class "popup"}
            ($ :h1 "Popup")
            ($ :p "Some text you might need for something or other."))))))

(defui broken [props]
  (throw (js/Error "Oh No"))
  ($ :<> "Never Getting Here"))

(defui control-flow-error-boundary []
  ($ :<>
    ($ :div "Before")
    ($ ErrorBoundary {:fallback (fn [err] err)}
      (fn [] ($ broken)))
    ($ :div "After")))

(defui lifecycles-on-mount []
  (let [[photos set-photos] (createSignal #js [])]
    (onMount (fn []
               (-> (js/fetch "https://jsonplaceholder.typicode.com/photos?_limit=4")
                   (.then (fn [res] (.json res)))
                   (.then set-photos))))
    ($ :<>
      ($ :h1 "Photo Album")
      ($ :div {:class "photos" :style "display: flex"}
        ($ For {:each photos :fallback ($ :p "Loading...")}
          (fn [^js photo]
            ($ :figure
              ($ :img {:src (.-thumbnailUrl photo) :alt (.-title photo)})
              ($ :figcaption (.-title photo)))))))))

(defui lifecycles-on-cleanup []
  ($ CountingComponent))

(defui bindings-events []
  (let [[pos set-pos] (createSignal {:x 0 :y 0})
        handle-mouse-move (fn [event]
                            (set-pos {:x (.-clientX event)
                                      :y (.-clientY event)}))]
    ($ :div {:onMouseMove handle-mouse-move
             :style #js {:width "100%"
                         :height "200px"
                         :background "#eee"}}
      "The mouse position is " #(:x (pos)) " x " #(:y (pos)))))

(defui bindings-styles []
  (let [[num set-num] (createSignal 0)
        interval (js/setInterval #(set-num (-> (num) (+ 5) (mod 255))) 30)]
    (onCleanup #(js/clearInterval interval))
    ($ :div {:style (fn [] #js {:color (str "rgb(" (num) ", 180, " (num) ")")
                                :font-weight 800
                                :font-size "32px"})}
                                ;:font-size (str (num) "px")})}
      "Some text")))

(defui bindings-class-list []
  (let [[current set-current] (createSignal "foo")]
    ($ :<>
      ($ :style ".selected { background-color: #ff3e00; color: white; }")
      ($ :button {:classList (fn [] #js {:selected (= (current) "foo")})
                  :onClick #(set-current "foo")}
        "foo")
      ($ :button {:classList (fn [] #js {:selected (= (current) "bar")})
                  :onClick #(set-current "bar")}
        "bar")
      ($ :button {:classList (fn [] #js {:selected (= (current) "baz")})
                  :onClick #(set-current "baz")}
        "baz"))))

(defn use-canvas-animation [canvas]
  (let [ctx (.getContext canvas "2d")
        !frame (atom nil)]
    (letfn [(loop-fn [t]
              (reset! !frame (js/requestAnimationFrame loop-fn))
              (let [image-data (.getImageData ctx 0 0 (.-width canvas) (.-height canvas))]
                (dotimes [i (-> image-data .-data .-length (/ 4))]
                  (let [p (* i 4)
                        x (mod i (.-width canvas))
                        y (Math/floor (/ i (.-height canvas)))
                        r (+ 64
                             (/ (* 128 x) (.-width canvas))
                             (* 64 (Math/sin (/ t 1000))))
                        g (+ 64
                             (/ (* 128 y) (.-height canvas))
                             (* 64 (Math/cos (/ t 1000))))
                        b 128]
                    (doto (.-data image-data)
                      (aset (+ p 0) r)
                      (aset (+ p 1) g)
                      (aset (+ p 2) b)
                      (aset (+ p 3) 255))))
                (.putImageData ctx image-data 0 0)))]
      (reset! !frame (js/requestAnimationFrame loop-fn))
      (onCleanup (fn [] (js/cancelAnimationFrame !frame))))))

(defui bindings-refs []
  (let [!canvas (atom nil)]
    (onMount #(use-canvas-animation @!canvas))
    ($ :div
      ($ :canvas {:width 256
                  :height 256
                  :ref (fn [el] (reset! !canvas el))}))))

(defui canvas [^js props]
  ($ :canvas {:ref (.-ref props) :width "256" :height "256"}))

(defui bindings-forwarding-refs []
  (let [!canvas (atom nil)]
    (onMount #(use-canvas-animation @!canvas))
    ($ :div
      ($ canvas {:ref (fn [el] (reset! !canvas el))}))))

(def pkg
  #js {:name "solid-js"
       :version 1
       :speed "⚡️"
       :website "https://solidjs.com"})

(defui info [^js props]
  ($ :p
    "The " ($ :code (.-name props)) " package is " (.-speed props) " fast. "
    "Download version " (.-version props) " from "
    ($ :a {:href (str "https://www.npmjs.com/package/" (.-name props))} "npm") " and "
    ($ :a {:href (.-website props)} "learn more here")))

(defui bindings-spreads []
  ($ info pkg))

(defn use-click-outside [el accessor]
  (let [on-click (fn [e]
                   (and (not (.contains el (.-target e)))
                        (accessor)))]
    (.addEventListener (.-body js/document) "click" on-click)
    (onCleanup #(.removeEventListener (.-body js/document) "click" on-click))))

;; No sugar like `use:clickOutside`, can be done using `ref`
(defui bindings-directives []
  (let [[show set-show] (createSignal false)]
    ($ :<>
      ($ :style ".modal {
                    padding: 16px;
                    border: 1px solid #444;
                    box-shadow: 4px 4px #88888866;
                  }")
      ($ Show {:when show
               :fallback ($ :button {:onClick #(set-show true)} "Open Modal")}
        ($ :div {:class "modal"
                 :ref (fn [el] (use-click-outside el #(set-show false)))}
          "Some Modal")))))

;; TODO figure out how to do destructuring
(defui greeting [^js props]
  #_(let [merged (mergeProps #js {:greeting "Hi" :name "John"} props)]
      ($ :h3 (.-greeting merged) " " (.-name merged)))
  ($ :h3 #(or (.-greeting props) "Hi") " " #(or (.-name props) "John")))

(defui props-default-props []
  (let [[name set-name] (createSignal)]
    ($ :<>
      ($ greeting {:greeting "Hello"})
      ($ greeting {:name "Jeremy"})
      ($ greeting {:name name})
      ($ :button {:onClick #(set-name "Jarod")}
        "Set Name"))))

;; TODO
(defui props-splitting-props [])

;; TODO
(defui props-children [])

(defui stores-nested-reactivity [])

(defui stores-create-store []
  (let [!input (atom nil)
        !todo-id (atom 0)
        [store set-store] (createStore #js {:todos #js [#js {:id -1
                                                             :text "hello"
                                                             :completed true}]})
        add-todo (fn [text]
                   (set-store
                    "todos"
                    (fn [todos]
                      (.concat todos #js {:id (swap! !todo-id inc)

                                          :text text
                                          :completed false}))))
        toggle-todo (fn [id]
                      (js/console.log "toggling" id)
                      (set-store
                       "todos"
                       (fn [todo] (= (.. todo -id) id))
                       "completed"
                       #(not %)))]
    ($ :<>
      ($ :div
        ($ :input {:ref #(reset! !input %)})
        ($ :button
          {:onClick (fn [_e]
                      (when-not (str/blank? (.. @!input -value))
                        (add-todo (.. @!input -value))
                        (set! (.. @!input -value) "")))}
          "Add Todo"))
      ($ For {:each #(.. store -todos)}
        (fn [todo]
          (js/console.log "Creating " (.. todo -text))
          ($ :div
            ($ :input {:type "checkbox"
                       :checked #(.. todo -completed)
                       :onchange #js [toggle-todo (.. todo -id)]})
            ($ :span {:style (fn [] #js {:text-decoration (if (.. todo -completed) "line-through" "none")})}
              (.. todo -text))))))))

;; TODO immer-style produce, likely more performant then re-creating the array
(defui stores-mutation [])

;; TODO - there should not be a gotcha with context
(defui stores-context [])

;; TODO - could try ratom instead of redux
;; reconcile function for reactive diffing
(defui stores-immutable [])

(defui main []
  ($ :<>
    ($ CountingComponent)
    ($ hello-world)
    ($ hello-world-svg)
    ($ counter)
    ($ control-flow-show)
    ($ control-flow-for)
    ($ control-flow-switch)
    ($ control-flow-dynamic)
    ($ control-flow-portal)
    ($ control-flow-error-boundary)
    ($ lifecycles-on-mount)
    ($ lifecycles-on-cleanup)
    ($ bindings-events)
    #_($ bindings-styles)
    ($ bindings-class-list)
    #_($ bindings-refs)
    #_($ bindings-forwarding-refs)
    ($ bindings-spreads)
    ($ bindings-directives)
    ($ props-default-props)
    ($ props-splitting-props)
    ($ props-children)
    ($ stores-nested-reactivity)
    ($ stores-create-store)
    ($ stores-mutation)))