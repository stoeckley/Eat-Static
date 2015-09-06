(ns eat-static.validations-test
  (:require [clojure.test :refer :all]
            [eat-static.validations :refer :all]))

(deftest invalid-df-calls
  (is (thrown? Exception (macroexpand '(df a b c [] :return))))
  (is (thrown? Exception (macroexpand '(df a (:a) [] :return))))
  (is (thrown? Exception (macroexpand '(df a b c [] :return))))
  (is (thrown? Exception (macroexpand '(df a b c [] :return))))
  (is (thrown? Exception (macroexpand '(df a b c [] :return))))
  (is (thrown? Exception (macroexpand '(df a b c [] :return))))
  (is (thrown? Exception (macroexpand '(df a b c [] :return))))
  (is (thrown? Exception (macroexpand '(df a b c [] :return)))))
