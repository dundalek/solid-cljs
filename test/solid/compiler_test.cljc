(ns solid.compiler-test
  (:require [solid.compiler :as compiler :refer [compile-static compile-template] :rename {compile-static $s compile-template $}]
            [clojure.test :refer [deftest are is]]
            [solid.web]
            ["solid-js" :as sj]
            [clojure.string :as str]
            [goog.object]
            [solid.core :refer [defc]]))

(deftest compile-static-test
  (are [expr expected] (= expected expr)
    ($s :span "Hello ")
    "<span>Hello </span>"

    ($s :span " John")
    "<span> John</span>"

    ($s :span "Hello   John") ;; unlike solid/JSX we could preserve whitespace in strings
    "<span>Hello   John</span>"

    ($s :span "Hello " name)
    "<span>Hello </span>"

    ($s :span greeting " John")
    "<span> John</span>"

    ($s :span greeting " " name)
    "<span> </span>"

    ($s :span " " greeting " " name " ")
    "<span> <!> <!> </span>"

    #_($s :span " " greeting "" name " ")
    ; ($s :span " " greeting name " ")
    ; "<span> <!> </span>"

    ($s :span "&nbsp;&lt;Hi&gt;&nbsp;")
    "<span>&nbsp;&lt;Hi&gt;&nbsp;</span>"

    ; ($s c "&nbsp;&lt;Hi&gt;&nbsp;")
    ; ($s :<> "&nbsp;&lt;Hi&gt;&nbsp;")
    ($s :span "Hi" "<script>alert();</script>")
    "<span>Hi&lt;script>alert();&lt;/script></span>"

    ; (let [value "World"]
    ;   ($s :span "Hello " (str value "!")))
    ; "<span>Hello World!</span>"

    ; (let [number (+ 4 5)]
    ;   ($s :span "4 + 5 = " number))
    ; "<span>4 + 5 = 9</span>"

    ($s :div s "\n" "d")
    "<div>\nd</div>"

    ($s :div expr)
    "<div></div>"))

    ; ($s c expr)
    ; ($s c {} expr)
    ;
    ; ($s :<> expr)

(defn- outer-html [el]
  (-> el
      .-outerHTML
      ;; striping placeholder comment nodes, find more rebust way
      (str/replace "<!---->" "")))

(deftest compile-template-test
  (let [[name _] (sj/createSignal "John")
        [greeting _] (sj/createSignal "Hello")]
    (is (= "<span>Hello John</span>"
           (outer-html ($ :span "Hello " name))))

    (is (= "<span>Hello John</span>"
           (outer-html ($ :span greeting " John"))))

    (are [expr expected] (= expected (outer-html expr))
      ($ :span greeting " " name)
      "<span>Hello John</span>"

      ($ :span " " greeting " " name " ")
      "<span> Hello John </span>"

      #_($s :span " " greeting "" name " ")
      ($ :span " " greeting name " ")
      "<span> HelloJohn </span>"

      ($ :div
        ($ :span "aaa")
        " "
        ($ :span "bbb"))
      "<div><span>aaa</span> <span>bbb</span></div>"

      ($ :div
        ($ :span greeting)
        " "
        ($ :span name))
      "<div><span>Hello</span> <span>John</span></div>")))

      ; ($ :<>
      ;   ($ :div "hello"))
      ; "<div>hello</div>")))

(deftest compile-attributes-test
  (are [expr expected] (= expected (outer-html expr))

    ($ :span {:class "abc"})
    "<span class=\"abc\"></span>"

    (let [cls "xyz"]
      ($ :span {:class cls}))
    "<span class=\"xyz\"></span>"

    (let [cls (constantly "abc")]
      ($ :span {:class (cls)}))
    "<span class=\"abc\"></span>"))

    ; ($ :span {:class-list {:abc true
    ;                        :xyz false}})
    ; "<span class=\"abc\"></span>"
    ;
    ; ($ :span {:style {:background-color "blue"
    ;                   :width "10px"}})
    ; "<span style=\"background-color:blue;width:10px\"></span>"))

#_(macroexpand '(solid.core/defc counter []
                  (solid.compiler/compile-template :div "hello")))




