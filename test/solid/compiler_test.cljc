(ns solid.compiler-test
  (:require [solid.compiler :refer [$]]
            [clojure.test :refer [deftest are]]))

(deftest compile
  (are [expr expected] (= expected expr)
    ($ :span "Hello ")
    "<span>Hello </span>"

    ($ :span " John")
    "<span> John</span>"

    ($ :span "Hello   John") ;; unlike solid/JSX we could preserve whitespace in strings
    "<span>Hello   John</span>"

    ($ :span "Hello " name)
    "<span>Hello </span>"

    ($ :span greeting " John")
    "<span> John</span>"

    ($ :span greeting " " name)
    "<span> </span>"

    ($ :span " " greeting " " name " ")
    "<span> <!> <!> </span>"

    #_($ :span " " greeting "" name " ")
    ($ :span " " greeting name " ")
    "<span> <!> </span>"

    ($ :span "&nbsp;&lt;Hi&gt;&nbsp;")
    "<span>&nbsp;&lt;Hi&gt;&nbsp;</span>"

    ; ($ c "&nbsp;&lt;Hi&gt;&nbsp;")
    ; ($ :<> "&nbsp;&lt;Hi&gt;&nbsp;")
    ($ :span "Hi" "<script>alert();</script>")
    "<span>Hi&lt;script>alert();&lt;/script></span>"

    (let [value "World"]
      ($ :span "Hello " (str value "!")))
    "<span>Hello World!</span>"

    (let [number (+ 4 5)]
      ($ :span "4 + 5 = " number))
    "<span>4 + 5 = 9</span>"

    ($ :div s "\n" "d")
    "<div>\nd</div>"

    ($ :div expr)
    "<div></div>"))

    ; ($ c expr)
    ; ($ c {} expr)
    ;
    ; ($ :<> expr)
