(ns solid.alpha.core
  (:require ["solid-js/h" :as hyperscript]
            [cljs-bean.core]
            [goog.object :as gobj]
            ["solid-js" :as s]
            [solid.alpha.impl.array :as array])
  (:require-macros [solid.alpha.core]))

#_(def proxy-props-handler
    #js {:get (fn [target prop]
                (fn [& args]
                  (let [x (gobj/get target prop)]
                    (if (fn? x)
                      (apply x args)
                      x))))})

(declare make-rbean)

(deftype RProps [m]
  ILookup
  (-lookup [_ k]
    (make-rbean
     (get m k))))

(deftype RBean [f]
  ILookup
  (-lookup [_ k]
    (make-rbean
     (s/createMemo
      (fn [] (get (f) k)))))
  #_(-lookup [_ k not-found])

  IFn
  (-invoke [_]
    (f))
  (-invoke [_ a]
    (f a))

  IDeref
  (-deref [_]
    (f)))

(defn make-rprops [m]
  (if (instance? RBean m)
    m
    (->RProps m)))

(defn make-rbean [x]
  (cond
    (instance? RBean x) x
    (not (fn? x)) (->RBean (fn [] x))
    (zero? (.-length x)) (->RBean x)
    :else x))

(defn wrap-rbean [x]
  ;; Wrapping so that solid does not treat it as plain object insted of callable
  (if (instance? RBean x)
    (fn [] (x))
    x))

(defn h [& args]
  (->> args
       (map wrap-rbean)
       (apply hyperscript)))

(def For s/For)
(def Show s/Show)

(def mapArray (.-mapArray ^js (array/solidInitArray s)))
; (def mapArray (.-mapArray ^js (js/window.solidInitArray s)))

(defn dispose [disposers]
  (doseq [d disposers]
    (d)))

(defn map-array [coll map-fn fallback]
  ; (solid/mapArray coll map-fn fallback)
  (mapArray coll map-fn fallback)
  #_(array/mapArray coll map-fn fallback)
  #_(let [;!items (atom #js []) ; probably needed for efficient reconciliation
          !mapped (atom #js [])
          !disposers (atom #js [])]
          ;;mapper (fn [disposers])]
      (solid/onCleanup #(dispose @!disposers))
      (fn []
        (let [new-items (or (coll) #js [])]

          (solid/untrack
            (fn []
              (dispose @!disposers)
              (reset! !disposers #js [])
              ; (reset! !mapped #js [])
              (reset! !mapped (js/Array.))
              (.forEach new-items
                (fn [item j]
                  ;; mapper
                  (aset @!mapped j
                        (solid/createRoot
                          (fn [dispose]
                            (aset @!disposers j dispose)
                            (map-fn item))))))
              @!mapped))))))

(defn reactive-for [items body]
  #_($js solid/For {:each items}
      body)
  (let [fallback js/undefined]
    (s/createMemo
      (fn []
        (map-array items body fallback)))))

