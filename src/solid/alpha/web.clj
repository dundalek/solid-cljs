(ns solid.alpha.web
  (:require [solid.alpha.compiler :as compiler]
            [clojure.walk :as walk]))

(defn- compile-all [form]
  (walk/prewalk (fn [x] (if (compiler/compilable-template? x) (compiler/compile-template* x) x))
                form))

;; TODO: needs to be smarter to handle docstrings, annotations, etc.
(defmacro defc [fn-name params & body]
  (let [!templates (atom {})
        component (binding [compiler/*templates* !templates]
                    (compile-all
                     (if (seq params)
                       `(defn ~fn-name [props#]
                          (let [~(first params) (solid.alpha.core/make-rprops props#)]
                            ~@body))
                       `(defn ~fn-name []
                          ~@body))))]
    `(do
       ~@(for [[tmpl-sym tmpl] @!templates]
           (list 'def tmpl-sym tmpl))
       ~component)))

(defmacro $ [& body]
  (compiler/compile-template* (conj body `compiler/compile-template)))
