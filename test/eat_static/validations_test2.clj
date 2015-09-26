(ns eat-static.validations-test2
  (:require [eat-static.validations :refer :all]
            [eat-static.validations-test :as vt]
            [clojure.test :refer :all]))


(deftest all-same-defaults
  (is (eval '(blend noy [-y] vt/y6)))
  (is (eval '(blend noy2 [[y]] vt/y6)))
  (is (eval '(blend noy3 [[y]] vt/noy2)))
  (is (eval '(blend noy4 [-y] vt/noy3)))
  (is (eval '(blend noy5 [-y] vt/noy)))
  (is (eval '(blend noy6 [-y] vt/y6a)))
  (is (= {:y 6} (d vt/y6a) (eval '(make vt/y6 {}))
         (eval '(d vt/noy)) (eval '(d vt/noy2)) (eval '(d vt/noy3))
         (eval '(d vt/noy4)) (eval '(d vt/noy5)) (eval '(d vt/noy6))))
  (is (thrown? AssertionError (eval '(mc vt/noy6 :x 3.3))))
  (is (= {:x 55 :y 6} (eval '(mc vt/noy6 :x 55)) (eval '(make vt/noy6 {:x 55}))))
  (is (thrown? AssertionError (eval '(mc vt/noy6 :w 1))))
  (is (= {:w 55.1 :y 6} (eval '(mc vt/noy6 :w 55.1)) (eval '(make vt/noy6 {:w 55.1}))))
  (is (thrown? AssertionError (eval '(make vt/noy6 {:w 2}))))
  (is (thrown? AssertionError (eval '(make vt/noy6 {:x 2.1}))))
  (is (false? (eval '(c vt/req? :w 12 :y 6))))
  (is (= {:w 1.1 :y 78} (eval '(mc vt/noy6 :w 1.1 :y 78)))))
