(ns demo.tutorial
  (:require
   [clojure.string :as str]
   ["solid-js" :refer [Show For Switch Match Dynamic Portal ErrorBoundary]]
   ["solid-js/store" :refer [createStore]]
   [solid.alpha.hyper :refer [defc $ $js]]
   [solid.alpha.core :as sc]))

(defc CountingComponent []
  (let [[count set-count] (sc/create-signal 0)
        interval (js/setInterval #(set-count inc) 1000)]
    (sc/on-cleanup #(js/clearInterval interval))
    ($ :div "Count value is " count)))

;; Tutorial

(defc hello-world []
  ($ :div "Hello Solid World!"))

(defc hello-world-svg []
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

(defc counter []
  (let [[count set-count] (sc/create-signal 0)
        double-count #(* (count) 2)]
    (sc/create-effect #(js/console.log "The count is now" (count)))
    ($ :div
      ($ :button {:on-click #(set-count inc)}
        "Click Me")
      ($ :div
        "Double: " (double-count)))))

(defc control-flow-show []
  (let [[logged-in set-logged-in] (sc/create-signal false)
        toggle #(set-logged-in (not (logged-in)))]
    ($js Show {:when logged-in
               :fallback ($ :button {:on-click toggle} "Log in")}
      ($ :button {:on-click toggle} "Log out"))))

(defc control-flow-for []
  (let [[cats _] (sc/create-signal
                  #js [{:id "J---aiyznGQ" :name "Keyboard Cat"}
                       {:id "z_AbfPXTKms" :name "Maru"}
                       {:id "OUtn3pvWmpg" :name "Henri The Existential Cat"}])]
    ($ :ul
      ($js For {:each cats}
        (fn [{:keys [name id]} i]
          ($ :li
            ($ :a {:target "_blank" :href (str "https://www.youtube.com/watch?v=" id)}
              (inc (i)) ": " name)))))))

(defc control-flow-switch []
  (let [[nums] (sc/create-signal #js [3 7 11])
        [x set-x] (sc/create-signal 11)]
    ($ :div
      ($js For {:each nums}
        (fn [num]
          ($ :button {:on-click #(set-x num)} num)))
      ($js Switch {:fallback ($ :p x " is between 5 and 10")}
        ($js Match {:when #(> (x) 10)}
          ($ :p (x) " is greater than 10"))
        ($js Match {:when #(> 5 (x))}
          ($ :p (x) " is less than 5"))))))

(defc red-thing []
  ($ :strong {:style "color: red"} "Red Thing"))

(defc green-thing []
  ($ :strong {:style "color: green"} "Green Thing"))

(defc blue-thing []
  ($ :strong {:style "color: blue"} "Blue Thing"))

(def options
  {"red" red-thing
   "green" green-thing
   "blue" blue-thing})

;; TODO
(defc control-flow-dynamic []
  (let [[selected set-selected] (sc/create-signal "red")]
    ($ :<>
      ($ :select {:value selected
                  :on-input (fn [^js e] (set-selected (.-currentTarget.value e)))}
        ($js For {:each (to-array (keys options))}
          (fn [color]
            ($ :option {:value color} color))))
       ;; Dynamic does not seem to work when backed with hyperscript
      #_($js Dynamic {:component (fn [] (options (selected)))})
      ($js Switch {:fallback ($ blue-thing)}
        ($js Match {:when #(= (selected) "red")} ($ red-thing))
        ($js Match {:when #(= (selected) "green")} ($ green-thing))))))

;; TODO
(defc control-flow-portal []
  #_($ :div {:class "app-container"}
      ($ :p "Just some text inside a div that has a restricted size.")
      ($js Portal
        (fn []
          ($ :div {:class "popup"}
            ($ :h1 "Popup")
            ($ :p "Some text you might need for something or other."))))))

(defc broken [props]
  (throw (js/Error "Oh No"))
  ($ :<> "Never Getting Here"))

(defc control-flow-error-boundary []
  ($ :<>
    ($ :div "Before")
    ($js ErrorBoundary {:fallback (fn [err] err)}
      ;; No extra function wrapping here needed, because of automatic fn wrapping of expressions
      ($ broken))
    ($ :div "After")))

(defc lifecycles-on-mount []
  (let [[photos set-photos] (sc/create-signal #js [])]
    (sc/on-mount (fn []
                   (-> (js/fetch "https://jsonplaceholder.typicode.com/photos?_limit=4")
                       (.then (fn [res] (.json res)))
                       (.then set-photos))))
    ($ :<>
      ($ :h1 "Photo Album")
      ($ :div {:class "photos" :style "display: flex"}
        ($js For {:each photos :fallback ($ :p "Loading...")}
          (fn [^js photo]
            ($ :figure
              ($ :img {:src (.-thumbnailUrl photo) :alt (.-title photo)})
              ($ :figcaption (.-title photo)))))))))

(defc lifecycles-on-cleanup []
  ($js CountingComponent))

(defc bindings-events []
  (let [[pos set-pos] (sc/create-signal {:x 0 :y 0})
        handle-mouse-move (fn [event]
                            (set-pos {:x (.-clientX event)
                                      :y (.-clientY event)}))]
    ($ :div {:on-mousemove handle-mouse-move
             :style #js {:width "100%"
                         :height "200px"
                         :background "#eee"}}
      "The mouse position is " #(:x (pos)) " x " #(:y (pos)))))

(defc bindings-styles []
  (let [[num set-num] (sc/create-signal 0)
        interval (js/setInterval #(set-num (-> (num) (+ 5) (mod 255))) 30)]
    (sc/on-cleanup #(js/clearInterval interval))
    ($ :div {:style (fn [] #js {:color (str "rgb(" (num) ", 180, " (num) ")")
                                :font-weight 800
                                :font-size "32px"})}
                                ;:font-size (str (num) "px")})}
      "Some text")))

(defc bindings-class-list []
  (let [[current set-current] (sc/create-signal "foo")]
    ($ :<>
      ($ :style ".selected { background-color: #ff3e00; color: white; }")
      ($ :button {:classList (fn [] #js {:selected (= (current) "foo")})
                  :on-click #(set-current "foo")}
        "foo")
      ($ :button {:classList (fn [] #js {:selected (= (current) "bar")})
                  :on-click #(set-current "bar")}
        "bar")
      ($ :button {:classList (fn [] #js {:selected (= (current) "baz")})
                  :on-click #(set-current "baz")}
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
      (sc/on-cleanup #(js/cancelAnimationFrame !frame)))))

(defc bindings-refs []
  (let [!canvas (atom nil)]
    (sc/on-mount #(use-canvas-animation @!canvas))
    ($ :div
      ($ :canvas {:width 256
                  :height 256
                  :ref (fn [el] (reset! !canvas el))}))))

(defc canvas [^js props]
  ($ :canvas {:ref (.-ref props) :width "256" :height "256"}))

(defc bindings-forwarding-refs []
  (let [!canvas (atom nil)]
    (sc/on-mount #(use-canvas-animation @!canvas))
    ($ :div
      ($ canvas {:ref (fn [el] (reset! !canvas el))}))))

(def pkg
  {:name "solid-js"
   :version 1
   :speed "⚡️"
   :website "https://solidjs.com"})

(defc info [{:keys [name speed version website] :as props}]
  ($ :p
    "The " ($ :code name) " package is " speed " fast. "
    "Download version " version " from "
    ($ :a {:href (str "https://www.npmjs.com/package/" name)} "npm") " and "
    ($ :a {:href website} "learn more here"))
  ;; TODO: make reactivity wrapping work for non-literal maps
  #_($ :p
      "The " ($ :code @name) " package is " @speed " fast. "
      "Download version " @version " from "
      ($ :a {:href (str "https://www.npmjs.com/package/" @name)} "npm") " and "
      ($ :a {:href @website} "learn more here")))

(defc bindings-spreads []
  ($ info pkg))

(defn use-click-outside [el accessor]
  (let [on-click (fn [e]
                   (and (not (.contains el (.-target e)))
                        (accessor)))]
    (.addEventListener (.-body js/document) "click" on-click)
    (sc/on-cleanup #(.removeEventListener (.-body js/document) "click" on-click))))

;; No sugar like `use:clickOutside`, can be done using `ref`
(defc bindings-directives []
  (let [[show set-show] (sc/create-signal false)]
    ($ :<>
      ($ :style ".modal {
                    padding: 16px;
                    border: 1px solid #444;
                    box-shadow: 4px 4px #88888866;
                  }")
      ($js Show {:when show
                 :fallback ($ :button {:on-click #(set-show true)} "Open Modal")}
        ($ :div {:class "modal"
                 :ref (fn [el] (use-click-outside el #(set-show false)))}
          "Some Modal")))))

(defc greeting [{:keys [greeting name]}]
  ;; mergeProps pattern is likely not needed if we have reactive maps as props
  #_(let [merged (mergeProps #js {:greeting "Hi" :name "John"} props)]
      ($ :h3 (.-greeting merged) " " (.-name merged)))
  ($ :h3 (or @greeting "Hi") " " (or @name "John")))

(defc props-default-props []
  (let [[name set-name] (sc/create-signal)]
    ($ :<>
      ($ greeting {:greeting "Hello"})
      ($ greeting {:name "Jeremy"})
      ($ greeting {:name name})
      ($ :button {:on-click #(set-name "Jarod")}
        "Set Name"))))

;; TODO
(defc props-splitting-props [])

;; TODO
(defc props-children [])

(defc stores-nested-reactivity [])

(defc stores-create-store []
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
          {:on-click (fn [_e]
                       (when-not (str/blank? (.. @!input -value))
                         (add-todo (.. @!input -value))
                         (set! (.. @!input -value) "")))}
          "Add Todo"))
      ($js For {:each #(.. store -todos)}
        (fn [todo]
          (js/console.log "Creating " (.. todo -text))
          ($ :div
            ($ :input {:type "checkbox"
                       :checked #(.. todo -completed)
                       :onchange #js [toggle-todo (.. todo -id)]})
            ($ :span {:style (fn [] #js {:text-decoration (if (.. todo -completed) "line-through" "none")})}
              (.. todo -text))))))))

;; TODO immer-style produce, likely more performant then re-creating the array
(defc stores-mutation [])

;; TODO - there should not be a gotcha with context
(defc stores-context [])

;; TODO - could try ratom instead of redux
;; reconcile function for reactive diffing
(defc stores-immutable [])

(defc main []
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
