(ns solid.core
  (:refer-clojure :exclude [if when for])
  (:require [solid.impl.core :as core]))

(defmacro defui1 [& body] (apply core/defui1 body))
(defmacro $1 [& body] (apply core/$1 body))
(defmacro $ [& body] (apply core/$ body))
(defmacro $js [& body] (apply core/$js body))
(defmacro defc [& body] (apply core/defc body))
(defmacro for [& body] (apply core/flow-for body))
(defmacro if [& body] (apply core/flow-if body))
(defmacro when [& body] (apply core/flow-when body))

