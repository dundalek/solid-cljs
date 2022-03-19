(ns solid.compiler
  (:require [clojure.string :as str]))

(defn escape [s]
  ;; TODO use proper escaping
  (str/replace s "<" "&lt;"))

(defn primitive? [x]
  (or (nil? x)
      (false? x)
      (number? x)
      (string? x)))

(defn trim [f coll]
  ;; TODO something less naive
  (->> coll
       (drop-while f)
       (reverse)
       (drop-while f)
       (reverse)))

(defn process-child [child]
  (cond (string? child) (escape child)
        (number? child) (str child)
        (or (nil? child) (false? child)) nil
        :else "<!>"))

(defmacro $ [el & body]
  (cond (keyword? el)
        (let [tag (name el)]
          (str "<" tag ">"
               (->> body
                    (trim (complement primitive?)) ; inserted values around edges don't need placeholder
                    ;; todo flatten multiple placeholders into one
                    (map process-child)
                    (apply str))
               "</" tag ">"))))

