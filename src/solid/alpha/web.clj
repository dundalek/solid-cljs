(ns solid.alpha.web
  (:require [solid.alpha.compiler :as compiler]))

(defmacro $ [& body]
  (compiler/compile-template* (conj body `compiler/compile-template)))
