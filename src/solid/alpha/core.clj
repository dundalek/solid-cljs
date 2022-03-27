(ns solid.alpha.core
  (:refer-clojure :exclude [if when for])
  (:require [solid.alpha.impl.core :as core]))

(defmacro for [& body] (apply core/flow-for body))
(defmacro if [& body] (apply core/flow-if body))
(defmacro when [& body] (apply core/flow-when body))
