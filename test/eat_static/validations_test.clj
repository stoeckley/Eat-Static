;;  Copyright (c) Andrew Stoeckley, 2015. All rights reserved.

;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the license file at the root directory of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns eat-static.validations-test
  (:require [clojure.test :refer :all]
            [eat-static.validations :refer :all]))

;; A mild handful of tests

(default-describe-names!)

;; all examples from the readme page
(deftest readme
  (is (df my-function [a b c] my-function-input))
  (is (defn my-function2 [{:keys [a b c] :as my-function2-input}]
        my-function2-input))
  (is (= (my-function {:a 1 :b 4 :c 98}) (my-function2 {:a 1 :b 4 :c 98})))
  (is (= (my-function {:a 1 :c 12 :b 4}) (c my-function :a 1 :c 12 :b 4)
         (c my-function2 :c 12 :b 4 :a 1)))
  (is (= (my-function {:a 1 :c 12 :b 4 :z :yo})
         (c my-function :z :yo :a 1 :c 12 :b 4)
         (c my-function2 :c 12 :z :yo :b 4 :a 1)))
  
  (is (df my-function3 [[a b c 0]] my-function3-input))
  (is (defn my-function4 [{:keys [a b c] :or {a 0 b 0 c 0}
                           :as my-function4-input}] my-function4-input))
  (is (= {} (my-function3 {}) (my-function4 {})))
  
  (is (df my-function3b [[a b c 0]] [a b c]))
  (is (defn my-function4b [{:keys [a b c] :or {a 0 b 0 c 0}
                            :as my-function4b-input}] [a b c]))
  (is (= [0 0 0] (my-function3b {}) (my-function4b {})))
  
  (is (df my-function5 [:int [a b c 0]] my-function5-input))
  (is (defn my-function6 [{:keys [a b c] :or {a 0 b 0 c 0}
                           :as my-function6-input}]
        {:pre [(integer? a) (integer? b) (integer? c)]}
        my-function6-input))
  (is (= {} (my-function5 {}) (my-function6 {})))
  
  (is (df my-function5b [:int [a b c 0]] my-function5b-input))
  (is (defn my-function6b [{:keys [a b c] :or {a 0 b 0 c 0}
                            :as my-function6b-input}]
        {:pre [(integer? a) (integer? b) (integer? c)]}
        my-function6b-input))
  (is (= {} (my-function5b {}) (my-function6b {})))
  (is (thrown? AssertionError (my-function5b {:a 1.1})))
  (is (thrown? AssertionError (my-function6b {:a 1.1})))
  (is (= {:a 1} (my-function5b {:a 1}) (my-function6b {:a 1})))
  
  (is (df my-function7 [:int [a b c 4]] [a b c]))
  (is (defn my-function8 [{:keys [a b c] :or {a 4 b 4 c 4}
                           :as my-function8-input}]
        {:pre [(integer? a) (integer? b) (integer? c)]}
        [a b c]))
  (is (= [4 4 4] (my-function7 {}) (my-function8 {})))

  (is (df my-function9 [a [b c 0]] :hi))
  (is (df my-function10 [a [b c]] :hi))
  (is (df my-function11 [:i a [b 5 c 6 d 7] e :n f] {:a a :b b :c c :d d :e e :f f}))
  (is (= :hi (my-function9 {:a 1}) (my-function10 {:a 88})))
  (is (thrown? AssertionError (my-function9 {:b 1 :c 1})))
  (is (thrown? AssertionError (my-function10 {:b 1 :c 1})))
  (is (thrown? AssertionError (my-function11 {:b 1 :c 1})))
  (is (thrown? AssertionError (my-function11 {:a 1.1 :e 4 :f 45 :b 1 :c 1})))
  (is (my-function11 {:a 1 :e 4 :f 45 :b 1 :c 1}))
  (is (thrown? AssertionError (my-function11 {:a 1 :b 1 :c 1})))
  (is (thrown? AssertionError (my-function11 {:a 1 :b 1 :e 1 :c 1})))
  (is (thrown? AssertionError (my-function11 {:a 1 :b 1 :c 1 :e 1 :f :hi})))
  (is (= {:a 99 :b 5 :c 6 :d 7 :e 88 :f 1.1}
         (my-function11 {:a 99 :e 88 :f 1.1})))
  (is (= {:a 99 :b 5 :c 6 :d 7 :e 88 :f 1.1}
         (c my-function11 :a 99 :e 88 :f 1.1)))
  (is (= {:a 99 :c 6 :d 7 :e 88 :f 1.1 :b -100}
         (c my-function11 :a 99 :e 88 :f 1.1 :b -100)))

  (is (df two-ints [(> b) a :i b (< 10) a b] [a b]))
  (is (= [5.5 5] (two-ints {:a 5.5 :b 5})))
  (is (= [9.5 9] (c two-ints :a 9.5 :b 9)))
  (is (= [8.123 -2222] (c two-ints :b -2222 :a 8.123)))
  (is (= [8 -2222] (c two-ints :b -2222 :a 8)))
  (is (thrown? AssertionError (c two-ints :b 5.5 :a 5.5)))
  (is (thrown? AssertionError (c two-ints :a 5.5 :b 10)))
  (is (thrown? AssertionError (c two-ints :a 10 :b 9)))
  (is (thrown? AssertionError (c two-ints :a 4 :b 5)))
  (is (= [5 4] (c two-ints :a 5 :b 4)))

  (is (df my-function in [a b c] in))
  (is (= {:a 1 :c :hello :b 2} (my-function {:a 1 :c :hello :b 2})))

  (is (df my-function "doc"[a b c] {:a a :c c :b b}))
  (is (= {:a 1 :c :hello :b 2} (my-function {:a 1 :c :hello :b 2})))

  (is (df my-function in "doc" [a b c] in))
  (is (= {:a 1 :c :hello :b 2} (my-function {:a 1 :c :hello :b 2})))

  (is (df my-function "doc" in [a b c] in))
  (is (= {:a 1 :c :hello :b 2} (my-function {:a 1 :c :hello :b 2})))

  (is (df validate-output (:i) [a] a))
  (is (= 5 (validate-output {:a 5})))
  (is (thrown? AssertionError (validate-output {:a 5.0})))

  (is (df another "integer less than 10" custom-input-map-name (:i (< 10)) [:i a b] (+ a b)))
  (is (= 5 (c another :a 2 :b 3)))
  (is (thrown? AssertionError (another {:a 5 :b 5})))

  (is (df another custom-input-map-name "integer less than 10" (:i (< 10)) [:i a b] (+ a b)))
  (is (= 5 (c another :a 2 :b 3)))
  (is (thrown? AssertionError (another {:a 5 :b 5})))

  (is (df another (:i (< 10)) [:i a b] (+ a b)))
  (is (= -25 (c another :a -22 :b -3)))
  (is (thrown? AssertionError (another {:a 5 :b 5})))

  (is (df has-keys-foo-bar ((:foo)(:bar)) [a] a))
  (is (df has2 ((pred> [foo bar])) [a] a))
  (is (df has3 (((predfn [foo bar]))) [a] a))
  (is (= {:foo 1 :bar 0} (has-keys-foo-bar {:a {:bar 0 :foo 1}})
         (has2 {:a {:bar 0 :foo 1}}) (has3 {:a {:bar 0 :foo 1}})
         (c has2 :a {:foo 1 :bar 0})))
  (is (thrown? AssertionError (has-keys-foo-bar {:a {:foo 1}})))
  (is (thrown? AssertionError (has2 {:a {:foo 1}})))
  (is (thrown? AssertionError (has3 {:a {:foo 1}})))

  (is (df has2 ((pred> [:i foo bar])) [a] a))
  (is (df has3 (((predfn [:i foo bar]))) [a] a))
  (is (thrown? AssertionError (has2 {:a {:foo 1 :bar 1.1}})))
  (is (thrown? AssertionError (has3 {:a {:foo 1 :bar 1.1}})))

  (is (df zero-or-one ((or> #(= 1 %) #(= 0 %))) [x] x))
  (is (thrown? AssertionError (c zero-or-one :x 2)))
  (is (= 1 (c zero-or-one :x 1)))

  (is (df out (:i (or> #(> % 10) #(< % -5))) [:i x y z] (+ x y z)))
  (is (defn out2
        [{:keys [x y z] :as out-input}]
        {:pre [(integer? x) (integer? y) (integer? z)]
         :post [(integer? %)
                (or (> % 10) (< % -5))]}
        (+ x y z)))
  (is (= (out {:x 5 :y 6 :z 7}) (c out2 :y 6 :x 5 :z 7)))
  (is (= (out {:x 5 :y 6 :z -70}) (c out2 :y 6 :x 5 :z -70)))
  (is (thrown? AssertionError (c out :x 1 :y 1 :z 1)))
  (is (thrown? AssertionError (c out2 :x 1 :y 1 :z 1)))

  (is (defn circle
        [{:keys [radius x y color] :or {color :blue}
          :as circle-input}]
        {:pre [(>= radius 1) (integer? x) (integer? y)
               (#{:blue :white} color)]}
        circle-input))
  (is (df circle2 [(>= 1) radius :i x y (#{:blue :white}) [color :blue]] circle2-input))
  (is (= (circle {:radius 2 :x 1 :y 1 :color :white})
         (circle2 {:radius 2 :x 1 :y 1 :color :white})
         (c circle2 :radius 2 :x 1 :y 1 :color :white)
         (c circle :radius 2 :x 1 :y 1 :color :white)
         {:radius 2 :x 1 :y 1 :color :white}))
  (is (thrown? AssertionError (circle2 {:radius 0.5 :x 1 :y 1 :color :white})))
  (is (thrown? AssertionError (circle2 {:radius 2 :x 1 :y 1 :color :green})))
  (is (thrown? AssertionError (circle2 {:radius 2 :x 1.0 :y 1 :color :white})))
  (is (thrown? AssertionError (circle {:radius 0.5 :x 1 :y 1 :color :white})))
  (is (thrown? AssertionError (circle {:radius 2 :x 1 :y 1 :color :green})))
  (is (thrown? AssertionError (circle {:radius 2 :x 1.0 :y 1 :color :white})))

  (is (defn circle
        [{:keys [radius x y z color] :or {color :blue}
          :as circle-input}]
        {:pre [(>= radius 1) (integer? x) (integer? y)
               (#{:blue :white} color)
               (if z (integer? z) true)]}
        circle-input))
  (is (df circle2 [(>= 1) radius :i x y -z (#{:blue :white}) [color :blue]] circle2-input))
  (is (= (circle {:radius 2 :x 1 :y 1 :color :white})
         (circle2 {:radius 2 :x 1 :y 1 :color :white})
         (c circle2 :radius 2 :x 1 :y 1 :color :white)
         (c circle :radius 2 :x 1 :y 1 :color :white)
         {:radius 2 :x 1 :y 1 :color :white}))
  
  (is (df intsgreater [#{:i (> 1)} x y z] [x y z]))
  (is (= [2 3 4] (c intsgreater :x 2 :y 3 :z 4)))
  (is (thrown? AssertionError (c intsgreater :x 2 :y 3 :z 1)))
  (is (thrown? AssertionError (c intsgreater :x 2 :y 3 :z 4.4)))

  (is (df i2 [(or> #(< 2 % 10) #(> -2 % -10)) x y] [x y]))
  (is (= [3 4] (i2 {:x 3 :y 4})))
  (is (thrown? AssertionError (i2 {:x 2 :y 4})))
  (is (thrown? AssertionError (i2 {:x -2 :y 4})))
  (is (= [-5 5] (c i2 :x -5 :y 5)))

  (is (df ki [(or> integer? keyword?) x] x))
  (is (df ki2 [(or> (t :i) (t :k)) x] x))
  (is (thrown? AssertionError (ki {:x 1.1})))
  (is (= :hi (ki {:x :hi})))
  (is (= 4 (c ki :x 4)))
  (is (thrown? AssertionError (ki2 {:x 1.1})))
  (is (= :hi (ki2 {:x :hi})))
  (is (= 4 (c ki2 :x 4)))
  (is (= (ki {:x :yo}) (ki2 {:x :yo})))

  (is (df int-stuff (:i (< 1)) [#{:i (or> #(< 2 % 10) #(> -2 % -10))} [x y 5]] (- x (* 2 y))))
  (is (= -5 (int-stuff {})))
  (is (= 0 (c int-stuff :x 8 :y 4)))
  (is (thrown? AssertionError (c int-stuff :x 10 :y 5)))
  (is (thrown? AssertionError (c int-stuff :x 8.5 :y 5)))
  (is (thrown? AssertionError (c int-stuff :x 9 :y 1)))
  (is (defn cint-stuff
        [{:keys [x y] :or {x 5 y 5}
          :as int-stuff-input}]
        {:pre [(integer? x) (integer? y)
               (or (< 2 x 10) (> -2 x -10))
               (or (< 2 y 10) (> -2 y -10))]
         :post [(integer? %) (< % 1)]}
        (- x (* 2 y)) ))
  (is (= -5 (cint-stuff {})))
  (is (= 0 (c cint-stuff :x 8 :y 4)))
  (is (thrown? AssertionError (c cint-stuff :x 10 :y 5)))
  (is (thrown? AssertionError (c cint-stuff :x 8.5 :y 5)))
  (is (thrown? AssertionError (c cint-stuff :x 9 :y 1)))

  (is (def sexes #{:male :female}))
  (is (df person [:str name (sexes) sex] person-input))
  (is (thrown? AssertionError (person {:name "andrew" :sex :wolf})))
  (is (= {:name "andrew" :sex :male} (person {:name "andrew" :sex :male})))

  (is (df is-senior? [:string name (> 65) age] true))
  (is (df process-seniors [(is-senior?) person1 person2] :cool))
  (is (def p1 {:name "john" :age 60}))
  (is (def p2 {:name "john" :age 66}))
  (is (def p3 {:name "john" :age :old}))
  (is (def p4 {:age 70}))
  (is (def p5 {:name :none :age 60}))
  (is (= :cool (c process-seniors :person1 p2 :person2 p2)))
  (is (= :cool (c process-seniors :person1 p2 :person2 {:name "andrew" :age 99})))
  (is (thrown? AssertionError (c process-seniors :person1 p2 :person2 p4)))
  (is (thrown? AssertionError (c process-seniors :person1 p2 :person2 p5)))
  (is (thrown? AssertionError (c process-seniors :person1 p2 :person2 p1)))

  (is (pred is-senior? [:string name (> 65) age]))
  (is (false? (is-senior? p1)))
  (is (false? (is-senior? p3)))
  (is (false? (is-senior? p4)))
  (is (false? (is-senior? p5)))
  (is (false? (is-senior? {:first-name "andrew" :age 99})))

  (is (false? ((predfn [a b]) {:a 1})))
  (is (false? (pred> {:a 1} [a b])))
  (is (df foo [((predfn [a b])) mymap] foo-input))
  (is (df foo2 [(pred> [a b]) mymap] foo2-input))
  (is (thrown? AssertionError (c foo :mymap {:a 1})))
  (is (thrown? AssertionError (c foo2 :mymap {:a 1})))

  (is (pred foo [(:a) mymap]))
  (is (false? (foo {:mymap 4})))
  (is (false? (foo {:mymap {:b 2}})))
  (is (c foo :mymap {:a :whatevs}))

  (is (df process-seniors [(#(every? is-senior? %)) persons] :nice))
  (is (thrown? AssertionError (c process-seniors :persons [p1 p2])))
  (is (= :nice (c process-seniors :persons [p2 {:name "andrew" :age 99}])))
  (is (pred is-awesome? [(= :super-cool) cool-factor]))
  (is (df process-awesome-seniors [(epcoll> is-senior? is-awesome?) persons] :wicked))
  (is (thrown? AssertionError (c process-awesome-seniors :persons [p2 {:name "andrew" :age 99}])))
  (is (def p2a (merge p2 {:cool-factor :super-cool})))
  (is (= :wicked (c process-awesome-seniors :persons
                    [p2a {:name "andrew" :age 99 :cool-factor :super-cool}])))

  (is (df all-have-a-b
          [(epcoll> (predfn [a b])) maps maps2] :yup))
  (is (= :yup (c all-have-a-b :maps [{:a 1 :b 2} {:a 5 :b 6 :c 7}]
                 :maps2 [{:a "hi" :b 11.1} {:b 6 :a :yo}])))
  (is (thrown? AssertionError (c all-have-a-b :maps [{:a 1 :b 2} {:a 5  :c 7}]
                                 :maps2 [{:a "hi" :b 11.1} {:b 6 :a :yo}])))

  (is (df intvec [(epcoll> integer?) v] :yippers))
  (is (df intvec2 [#{:v (epcoll> integer?)} v] :uh-huh))
  (is (df intvec2b [(epv> (t :i)) v] :uh-huh))
  (is (= :yippers (c intvec :v [1 2 3 4 -2 -123 0])))
  (is (= :yippers (c intvec :v (list 1 2 3 4 -2 -123 0))))
  (is (= :yippers (c intvec :v '(1 2 3 4 -2 -123 0))))
  (is (= :uh-huh (c intvec2 :v [1 2 3 4 -2 -123 0])))
  (is (thrown? AssertionError (c intvec2 :v '(1 2 3 4 -2 -123 0))))
  (is (thrown? AssertionError (c intvec :v '(1 2 3 4 -2 -123 1.0))))
  (is (= :uh-huh (c intvec2b :v [1 2 3 4 -2 -123 0])))
  (is (thrown? AssertionError (c intvec2b :v '(1 2 3 4 -2 -123 0))))

  (is (df vari [(or> #(epcoll> % (t :i)) #(epcoll> % (t :k))) v] :yes))
  (is (= :yes (c vari :v [1 5 2 32 4 -123 3 5 4 5])))
  (is (= :yes (c vari :v [:nice :dude])))
  (is (thrown? AssertionError (c vari :v [1 5 2 :hi -123 :whatever-man])))
  (is (thrown? AssertionError (c vari :v ["hi" "there"])))

  (is (defn vec-of-ints [x] (epv> x (t :i))))
  (is (df many-vecs [(vec-of-ints) a b c] :indeed))
  (is (pred many-vecs-p [(vec-of-ints) a b c]))
  (is (= :indeed (many-vecs {:a [2 -2] :b [66 -9876562] :c [0]})))
  (is (false? (many-vecs-p {:a [2 -2] :b [66 -9876562] :c [0.1]})))
  (is (false? (many-vecs-p {:a [2 -2] :b [66 -9876562 :hi] :c [0]})))
  (is (thrown? AssertionError (many-vecs {:a [2 -2] :b [66 -9876562] :c [0.1]})))
  (is (thrown? AssertionError (many-vecs {:a [2 -2] :b [66 -9876562 :hi] :c [0]})))

  (is (pred kitty [:k color :i age :b likes-milk]))
  (is (defn bunch-o-kitties [ks] (epv> ks kitty)))
  (is (df feed-kitties [(bunch-o-kitties) cats] :fed))
  (is (= :fed (c feed-kitties :cats [{:color :blue :age 0 :likes-milk true}])))
  (is (= :fed (c feed-kitties :cats [{:color :green :age 10 :likes-milk false}
                                     {:color :blue :age 0 :likes-milk true}])))
  (is (thrown? AssertionError (c feed-kitties :cats [{:color :blue :age 0}])))
  (is (thrown? AssertionError (c feed-kitties :cats [{:color :blue :likes-milk true :age 0.5}])))
  (is (thrown? AssertionError (c feed-kitties :cats [{:color "blue" :age 0 :likes-milk true}])))

  (is (df make-person [:bool eats-meat :str name [spouse] country :i age :k education]
          make-person-input))
  (is (describe dperson [:bool eats-meat :str name [spouse] country :i age :k education]))
  (is (pred person? [:bool eats-meat :str name [spouse] country :i age :k education]))
  (is (describe child [:str name fav-toy :i age :k education]))
  (is (make-person {:name "Ludwig" :spouse "Angela" :education :college
                    :country "USA" :age 39 :eats-meat false}))
  (is (make-person {:name "Bobby" :country "USA" :age 4 :eats-meat true :education :pre-school}))
  (is (thrown? AssertionError (make-person {:name "Bobby" :country "USA" :age 4 :eats-meat true})))
  (is (false? (person? {:name "Bobby" :country "USA" :age 4 :eats-meat true})))
  (is (false? (dperson? {:name "Bobby" :country "USA" :age 4 :eats-meat true})))
  (is (person? {:name "Bobby" :country "USA" :age 4 :eats-meat true :education :pre-school}))
  (is (dperson? {:name "Bobby" :country "USA" :age 4 :eats-meat true :education :pre-school}))
  (is (= (make-person {:name "Ludwig" :spouse "Angela" :education :college
                       :country "USA" :age 39 :eats-meat false})
         (make-dperson {:name "Ludwig" :spouse "Angela" :education :college
                        :country "USA" :age 39 :eats-meat false})))
  (is (def alex (make-child {:name "alex" :fav-toy "legos" :age 8 :education :primary})))
  (is (child? alex))
  (is (describe baby-child [(child?) baby-child-input (< 2) age]))
  (is (describe baby-child2 [(child?) baby-child2-input (< 2) age] "new-" ""))
  (is (child? (make-baby-child (assoc alex :age 1))))
  (is (baby-child2 (new-baby-child2 (assoc alex :age 1))))

  (is (describe defaults [:i a [b 8 c 9]]))
  (is (= (make-defaults {:a 1}) {:a 1 :b 8 :c 9}))
  (is (= (make-defaults {:a 1 :b 2}) {:a 1 :b 2 :c 9}))
  (is (nil? (set-describe-names! "front" "back")))
  (is (desc fb [:k q w e [r :whatever]]))
  (is (= {:q :ui :w :w :e :e :r :whatever} (make fb {:q :ui :w :w :e :e})))
  (is (is? fb {:q :ui :w :w :e :hi :r :yupper}))
  (is (false? (is? fb {:q 2 :w :w :e :hi :r :yupper})))
  (is (nil? (default-describe-names!)))

  (is (do (desc person [:str name :i age :k sex :n height])
          (desc tall [(> 2) height])
          (desc tall-person [(ep> person? tall?) tall-person-input])
          (desc short-person [(person?) short-person-input (< 1) height])
          (desc tall-person-bobby [(tall-person?) tall-person-bobby-input (= "bobby") name])
          (desc child [(person?) child-input (< 27) age])
          (desc short-child [(child?) short-child-input (< 0.8) height])))
  (is (thrown? AssertionError (make-short-child {:name "andrew" :age 12 :height 1.5 :sex :m})))
  (is (short-child? {:name "andrew" :age 12 :height 0.7 :sex :m}))

  (is (do (pred is-dutch? [(= "netherlands") country])
          (pred is-senior? [(> 65) age])
          (pred is-vegetarian? [(= false) eats-meat])
          (pred elder-dutch-vegetarian? person
                [(person?) person (is-senior?) person (is-vegetarian?) person (is-dutch?) person])
          (pred elder-dutch-vegetarian2? person
                [((every-pred person? is-senior? is-vegetarian? is-dutch?)) person])
          (pred elder-dutch-vegetarian3? person [(ep> person? is-senior? is-vegetarian? is-dutch?) person])
          (def old-dutch-veggie-eater? (every-pred person? is-senior? is-vegetarian? is-dutch?))
          (def veggie-eater? (every-pred person? is-vegetarian?))
          (def young-veggie-eater? (every-pred person? (complement is-senior?) is-vegetarian?))
          (def young-veggie-eater2? (every-pred person? (predfn [(< 20) age]) is-vegetarian?))
          (pred young-veggie-eater3? m [(ep> person? is-vegetarian?) m (< 20) age])
          (df invite-to-amsterdam-elder-poker-party [(is-senior?) person (is-dutch?) person] :invited)
          (pred is-dutch-senior? [(is-senior?) person (is-dutch?) person])
          (pred is-dutch-senior2? [#{(is-dutch?)(is-senior?)} person])
          (def hank {:country "netherlands" :age 67 })
          (describe person2 [:str name country :i age :k sex :n height :b eats-meat])
          (pred american-meat-eating-child? [(< 10) age (= true) eats-meat (= "USA") country])
          (def jimmy (make-person {:age 5 :eats-meat true :country "USA" :height 1 :sex :m :name "jimmy"}))
          (pred american-child-likes-meat? [(person?) kid (american-meat-eating-child?) kid])
          (pred american-child-likes-meat2? kid [(person?) kid (american-meat-eating-child?) kid])))

  (is (false? (american-meat-eating-child? {:age 50 :eats-meat true :country "USA"})))
  (is (c is-dutch-senior? :person hank))
  (is (false? (c is-dutch-senior? :person (assoc hank :age 1))))
  (is (false? (c is-dutch-senior? :person (assoc hank :age :young))))
  (is (american-meat-eating-child? jimmy))
  (is (c american-child-likes-meat? :kid jimmy))

  ;; note that in some cases, testing a macro here requires use of eval to
  ;; full expand the macro before testing inside the clojure.test macros
  
  (is (do (describe ab [:i q [a 1 b 2]])
          (desc cd [:f w -r :any [c 3 d 4]])
          (blend ab-cd [:k e] ab cd)))
  (is (thrown? AssertionError (eval '(make-ab-cd {:e :hi}))))
  (is (thrown? AssertionError (eval '(make-ab-cd {:e :hi :w 1.1}))))
  (is (= {:e :hi, :w 1.1, :q 55, :a 1, :c 3, :b 2, :d 4}
         (eval '(make-ab-cd {:e :hi :w 1.1 :q 55}))))

  (is (eval '(do (desc red-rectangle [:n [width 5 height 3] :k [color :red]])
                 (desc square [:n [width 5 height 5]])
                 (blend red-square [] red-rectangle square))))
  (is (= (eval '(make-red-square {})) {:width 5, :height 5, :color :red}))
  (is (eval '(blend red-square [] square red-rectangle)))
  (is (= (eval '(make-red-square {})) {:color :red, :width 5, :height 3}))
  (is (eval '(blend blue-square [[color :blue]] square)))
  (is (= (eval '(make-blue-square {})) {:width 5, :height 5, :color :blue}))
  (is (eval '(red-square? (make-blue-square {}))))

  (is (eval '(blend red-square [(= :red) [color :red]] square)))
  (is (eval '(blend tiny-red-square [#{:f (< 1)} [width height 0.01]] red-square)))
  (is (= (eval '(make-tiny-red-square {})) {:width 0.01, :height 0.01, :color :red}))
  (is (false? (eval '(is? tiny-red-square {:color :green}))))
  (is (false? (eval '(tiny-red-square? (eval '(make-blue-square {}))))))
  (is (eval '(tiny-red-square? {:color :red})))
  (is (eval '(tiny-red-square? {})))

  (is (eval '(blend tiny-green-square [[color :green]] tiny-red-square)))
  (is (thrown? AssertionError (eval '(make-tiny-green-square {}))))
  (is (= {:color :red :width 0.01 :height 0.01}
         (eval '(make-tiny-green-square {:color :red}))))

  (is (eval '(blend red-square [{color :red}] square)))
  (is (eval '(blend tiny-red-square [#{:f (< 1)} [width height 0.01]] red-square)))
  (is (= (eval '(make-tiny-red-square {})) {:width 0.01, :height 0.01, :color :red}))
  (is (thrown? AssertionError (eval '(make-tiny-red-square {:color :green}))))
  (is (false? (eval '(tiny-red-square? {:color :green}))))
  (is (false? (eval '(tiny-red-square? (eval '(make-blue-square {}))))))
  (is (eval '(tiny-red-square? {:color :red})))
  (is (eval '(tiny-red-square? {})))
  (is (eval '(blend tiny-green-square [[color :green]] tiny-red-square)))
  (is (thrown? AssertionError (eval '(make-tiny-green-square {}))))
  (is (= {:color :red :width 0.01 :height 0.01}
         (eval '(make-tiny-green-square {:color :red}))))

  (is (do (eval '(blend red-square-1 [{width 1 height 1}] red-square))
          (eval '(desc two-red-square-1 [{one red-square-1 two red-square-1}]))))
  (is (= (eval '(make-red-square-1 {})) {:width 1, :height 1, :color :red}))
  (is (= (eval '(make-two-red-square-1 {})) {:one {:width 1, :height 1, :color :red},
                                             :two {:width 1, :height 1, :color :red}}))
  (is (false? (eval '(two-red-square-1? {:one {:width 1, :height 2, :color :red},
                                         :two {:width 1, :height 1, :color :red}}))))

  (is (thrown? AssertionError (def some-square (eval '(make-red-square-1 {:width 2 :height 2})))))
  (is (def john-square (eval '(make-red-square-1 {:owner "John"}))))
  (is (eval '(red-square-1? {:a 2 :b 3})))
  (is (false? (eval '(red-square-1? {:a 2 :b 3 :color :green}))))
  (is (= (eval '(make-red-square-1 {:a 2 :b 3})) {:a 2, :b 3, :width 1, :height 1, :color :red}))

  (is (desc baby-white-kitty [:k [color :white] :i [age 0] :b [likes-milk true]]))
  (is (= (d baby-white-kitty) {:color :white, :age 0, :likes-milk true}))
  (is (def feline-litter (dv baby-white-kitty 5)))
  (is (= feline-litter [{:color :white, :age 0, :likes-milk true}
                        {:color :white, :age 0, :likes-milk true}
                        {:color :white, :age 0, :likes-milk true}
                        {:color :white, :age 0, :likes-milk true}
                        {:color :white, :age 0, :likes-milk true}]))

  (is (do (desc car [:i [age 0] :str [make "GM"]])
          (desc new-car-purchase [:str store (car?) [new-car (d car)]])
          (desc new-car-purchase2 [:str store {new-car car}])))
  (is (= (c make-new-car-purchase :store "Al's Car Shop")
         {:store "Al's Car Shop", :new-car {:age 0, :make "GM"}}))
  (is (desc factory-output [(epv> car?) [cars (dv car 5)]]))
  (is (= (make-factory-output {})
         {:cars
          [{:age 0, :make "GM"}
           {:age 0, :make "GM"}
           {:age 0, :make "GM"}
           {:age 0, :make "GM"}
           {:age 0, :make "GM"}]}))
  (is (= (make-factory-output {:cars (dv car 2)})
         {:cars [{:age 0, :make "GM"}
                 {:age 0, :make "GM"}]}))
  (is (= (vmake car {:color :white} 5)
         [{:color :white, :age 0, :make "GM"}
          {:color :white, :age 0, :make "GM"}
          {:color :white, :age 0, :make "GM"}
          {:color :white, :age 0, :make "GM"}
          {:color :white, :age 0, :make "GM"}]))

  (is (desc person [:str name spouse country :b [eats-meat false] :k -sex :n -height]))
  (is (df married? [(person?) wife husband] (= (:name husband) (:spouse wife))))
  (is (pred married2? [(person?) husband wife (#(= (:name husband) (:spouse %))) wife])   )
  (is (pred dutch? [(person?) dutch?-input (= "netherlands" country)]))
  (is (pred married-dutch? in [(married?) in (dutch?) wife husband]))
  (is (pred married-dutch-c? [(c> married? :wife :husband husband) wife (dutch?) wife husband]))
  (is (def andrew (make-person {:name "andrew" :spouse "christine" :country "netherlands"})))
  (is (def bobby (make-person {:name "bobby" :spouse "alice" :country "netherlands"})))
  (is (def christine (make-person {:name "christine" :spouse "andrew" :country "netherlands"})))
  (is (false? (c married2? :husband bobby :wife christine)))
  (is (married2? {:husband andrew :wife christine}))
  (is (not (false? (c married-dutch? :wife christine :husband andrew ))))
  (is (false? (married-dutch? {:c {:wife christine :husband bobby}})))
  (is (not (false? (c married-dutch-c? :wife christine :husband andrew ))))
  (is (false? (married-dutch-c? {:c {:wife christine :husband bobby}})))

  (is (df old-dutch-vegetarian-spouses? [(person?) husband wife]
          (and (c married? :husband husband :wife wife)
               (elder-dutch-vegetarian? husband)
               (elder-dutch-vegetarian? wife))))
  (is (df old-dutch-vegetarian-spouses2? in [(person?) husband wife]
          (and (married? in) ((predfn [(elder-dutch-vegetarian?) husband wife]) in))))
  (is (pred old-dutch-vegetarian-spouses3? [(elder-dutch-vegetarian?) husband wife 
                                            (c> married? :husband :wife wife) husband]))
  (is (pred old-dutch-vegetarian-spouses4? [(elder-dutch-vegetarian?) husband wife 
                                            (c> married? :wife :husband husband) wife]))
  (is (pred old-dutch-vegetarian-spouses5? in [(elder-dutch-vegetarian?) husband wife (married?) in]))
  (is (do (def alice (make-person {:name "alice" :spouse "jon" :country "netherlands" :age 99
                                   :sex :female :height 1.4}))
          (def jon (make-person {:name "jon" :spouse "alice" :country "netherlands" :age 97
                                 :sex :male :height 2}))
          (def mike (make-person {:name "mike" :spouse "alice" :country "netherlands" :age 97
                                 :sex :male :height 2}))))
  (is (old-dutch-vegetarian-spouses2? {:husband jon :wife alice}))
  (is (old-dutch-vegetarian-spouses? {:husband jon :wife alice}))
  (is (old-dutch-vegetarian-spouses3? {:husband jon :wife alice}))
  (is (old-dutch-vegetarian-spouses4? {:husband jon :wife alice}))
  (is (old-dutch-vegetarian-spouses5? {:husband jon :wife alice}))
  (is (false? (old-dutch-vegetarian-spouses2? {:husband mike :wife alice})))
  (is (false? (c old-dutch-vegetarian-spouses? :wife alice :husband mike)))
  (is (false? (old-dutch-vegetarian-spouses3? {:husband mike :wife alice})))
  (is (false? (c old-dutch-vegetarian-spouses4? :wife alice :husband mike)))
  (is (false? (c old-dutch-vegetarian-spouses5? :wife alice :husband mike))))

(deftest df-output-keys
  (is (df a ((:b)) [c] c))
  (is (= {:b 5} (c a :c {:b 5})))
  (is (thrown? AssertionError (c a :c 5)))
  (is (thrown? AssertionError (c a :c {:c 1})))
  (is (df a ((:b) (:c)) [c] c))
  (is (= {:b 5 :c :hi} (c a :c {:b 5 :c :hi})))
  (is (thrown? AssertionError (c a :c 5)))
  (is (thrown? AssertionError (c a :c {:c 1})))
  (is (thrown? AssertionError (c a :b 5)))
  (is (thrown? AssertionError (c a :c {:b 1})))
  (is (df a ((pred> [b c])) ain [c] c))
  (is (= {:b 5 :c :hi} (c a :c {:b 5 :c :hi})))
  (is (thrown? AssertionError (c a :c 5)))
  (is (thrown? AssertionError (c a :c {:c 1})))
  (is (thrown? AssertionError (c a :b 5)))
  (is (thrown? AssertionError (c a :c {:b 1})))
  (is (df a ((pred> [b c])) ain [c] (assoc c :b 1 :c 1)))
  (is (= {:b 1 :c 1} (c a :c {:b 5 :c :hi})))
  (is (= {:b 1 :c 1} (a {:c {}})))
  (is (= {:b 1 :c 1} (a {:b 6 :c {}})))
  (is (thrown? AssertionError (a {:b 6})))
  (is (thrown? AssertionError (a [])))
  (is (thrown? AssertionError (c a :b 6)))
  (is (thrown? ClassCastException (c a :c 7)))
  (is (= {:b 1 :c 1 :z 6} (c a :c {:z 6}))))

(deftest df-output-keys-nested-validations
  (is (df a ((pred> [#{:i (< 10)} b :k c])) "complex" ain [c] c))
  (is (= {:b 5 :c :hi} (c a :c {:b 5 :c :hi})))
  (is (thrown? AssertionError (a {:b :hi})))
  (is (thrown? AssertionError (c a :c 7)))
  (is (thrown? AssertionError (c a :c {})))
  (is (thrown? AssertionError (a {:c {:b 15 :c :hi}})))
  (is (thrown? AssertionError (a {:c {:b 5 :c 4}}))))

(deftest df-output-int
  (is (df a (:i) [b] (* b 2)))
  (is (df b (:i) [c] (int (* 2.0 c))))
  (is (thrown? AssertionError (c a :b 1.2)))
  (is (thrown? AssertionError (c a :b -123.0)))
  (is (thrown? AssertionError (a {:b 1.2})))
  (is (thrown? AssertionError (c a :dd 1)))
  (is (thrown? AssertionError (c a :c 1.2)))
  (is (thrown? AssertionError (c a :c 1)))
  (is (= 10 (c a :b 5)))
  (is (= 10 (c a :b (int 5.9))))
  (is (= 20 (a {:b 10})))
  (is (= 40 (c b :c 20.1))))

(deftest df-output-int-validations
  (is (df a (:i (or> #(< % 0) #(< 10 %))
                (ep> #(or (= 100 (+ 10 %)) (zero? (- % 20))
                          (zero? (- % 5)) (zero? (- % 50))
                          (zero? (- % 200)))
                     #(< % 100)))
          "complex"
          [b] b))
  (is (= 20 (c a :b 20)))
  (is (= 90 (a {:b 90})))
  (is (= 50 (a {:b 50})))
  (is (thrown? AssertionError (c a :c 200)))
  (is (thrown? AssertionError (c a :c 20)))
  (is (thrown? AssertionError (c a :b 60)))
  (is (thrown? AssertionError (c a :b 5)))
  (is (df b bin "simpler" (:f (< 10)) [c] c))
  (is (= 5.0 (c b :c 5.0)))
  (is (= -1.234 (c b :c -1.234)))
  (is (thrown? AssertionError (c b :c 5)))
  (is (thrown? AssertionError (c b :c 10.0)))
  (is (thrown? ClassCastException (c b :c [5]))))

(deftest df-input-int
  (is (df a [:i b] (* b 3)))
  (is (thrown? AssertionError (c a :b 1.2)))
  (is (thrown? AssertionError (c a :b -1.2)))
  (is (thrown? AssertionError (a {:b 1.2})))
  (is (= 9 (c a :b 3))))

(deftest df-input-float-default
  (is (df as in [(pred> [:i [a 4.5]]) in]))
  (is (df as2 [(pred> [:i [a 4.5]]) as-input]))
  (is (thrown? AssertionError (as {})))
  (is (thrown? AssertionError (as2 {})))
  (is (df a "testing float" [:f [b 2.2 c 3.3 ]] (+ b c)))
  (is (df b bin "testing opt" [:f -c] (if c (* 2 c))))
  (is (df cc  [:f [b 2 c 3.3 ]] (+ b c)))
  (is (thrown? AssertionError (cc {:c 2.2})))
  (is (= 5.0 (c cc :b 2.0 :c 3.0)))
  (is (= 5.5 (a {})))
  (is (= 5.5 (a {:something :else})))
  (is (= 4.4 (a {:b 1.1 :z 0})))
  (is (= 4.4 (c a :c 2.2)))
  (is (= 10.0 (c a :b 6.5 :c 3.5)))
  (is (= 10.0 (c a :b 7.0 :c 3.0)))
  (is (= 0.0 (a {:b 10.0 :c -10.0})))
  (is (= 4.0 (c b :c 2.0)))
  (is (thrown? AssertionError (c a :b 1)))
  (is (thrown? AssertionError (c a :b 1 :z 2.2)))
  (is (thrown? AssertionError (c a :c 4))))

(deftest df-pre-post
  (is (df a [b] {:pre [(= b 5)]} b))
  (is (thrown? AssertionError (c a :b 3)))
  (is (= 5 (c a :b 5)))
  (is (df a [b] {:post [(= b 5)]} b))
  (is (thrown? AssertionError (c a :b 3)))
  (is (= 5 (c a :b 5))))

(deftest pred-keys
  (is (pred a [b c -d [e 9]]))
  (is (not (false? (c a :b 1 :c 1))))
  (is (not (false? (c a :b 1 :c 1 :d 1 :e 1))))
  (is (false? (a {:b 1})))
  (is (false? (c a :b 1 :d 3 :e 5 :g 5))))

(deftest pred-int
  (is (pred a [:i b c]))
  (is (not (false? (c a :b 1 :c 2))))
  (is (false? (c a :b 2)))
  (is (false? (c a :c 2)))
  (is (false? (c a :b 1.1 :c 0.0)))
  (is (not (false? (a {:b 0 :c 0 :d 2.3 :e :hi})))))

(deftest pred-types
  (is (pred m [:i a :f b :k c :m d :b e :v f :i g]))
  (is (false? (m {})))
  (is (not (false? (c m :a 1 :b 1.0 :c :hi :d {1 2} :e true :f [:a] :g 1))))
  (is (false? (c m :a 1 :b 1.0 :c :hi :d {1 2} :e true :f [:a] :g 1.1)))
  (is (false? (c m :a 1 :b 1.0 :c :hi :d {1 2} :e true :f 2 :g 1.1)))
  (is (false? (c m :a 1 :b 1.0 :c :hi :d {1 2} :e true :f {} :g 1)))
  (is (false? (c m :a 1 :b 1.0 :c :hi :d {1 2} :e 2 :f [:a] :g 1)))
  (is (false? (c m :a 1 :b 1.0 :c :hi :d :hi :e true :f [:a] :g 1)))
  (is (false? (c m :a 1 :b 1.0 :c 1 :d {1 2} :e true :f [:a] :g 1)))
  (is (false? (c m :a 1 :b 1.0 :c :hi :d {1 2} :e true :f [:a])))
  (is (false? (c m :a 1.0 :b 1.0 :c :hi :d {1 2} :e true :f [:a] :g 1))))

(deftest default-conflicts
  (is (desc aaa [:i -j]))
  (is (not (false? (aaa? {}))))
  (is (not (false? (aaa? {:j 44}))))
  (is (false? (aaa? {:j 44.1})))
  (is (desc baa [:i -j [i 9.1]]))
  (is (not (false? (baa? {:j 1 :i 4}))))
  (is (false? (baa? {:j 1}))))

(deftest pred-validations
  (is (pred m [#{:i :n (> 20)} a b :f c -d]))
  (is (not (false? (c m :a 21 :b 1234567 :c -0.0))))
  (is (not (false? (c m :a 21 :b 1234567 :c -0.0 :d -111.111111))))
  (is (false? (c m :a 21 :b 1234567 :c -0.0 :d -1)))
  (is (false? (c m :a 21 :b 1234567 :c -0 :d -1.1)))
  (is (false? (c m :a 21 :b 1234567 :d -1)))
  (is (false? (c m :a 20 :b 1234567 :c -0.0 :d -1)))
  (is (false? (c m :a 21 :b 12 :c -0.0 :d -1)))
  (is (pred m [(or> #(= % 5) #(= 2.2 %) #(= :hi %) #(> 1000 %)) a -b c]))
  (is (not (false? (m {:a 5 :b :hi :c 999}))))
  (is (not (false? (m {:a 5 :c 999}))))
  (is (not (false? (m {:a 2.2 :c :hi}))))
  (is (not (false? (m {:a :hi :b :hi :d [] :c :hi}))))
  (is (not (false? (m {:a :hi :c 2.2}))))
  (is (false? (c m :a 5 :b 2)))
  (is (false? (c m :a 5 :b :hi)))
  (is (false? (c m :b :hi :c :hi)))
  (is (false? (c m :b :hi :c 1000)))
  (is (false? (c m :a 5 :b 2000 :c :hi)))
  (is (pred m [#{:i (ep> #(> % 5) #(< % 10))} a b -c]))
  (is (not (false? (c m :a 6 :b 6 :c 6))))
  (is (false? (c m :a 6 :b 5 :c 6)))
  (is (false? (c m :a 6 :b 6.0 :c 6))))

(deftest pred-nested
  (is (pred m [(pred> [:i a :k b]) c]))
  (is (not (false? (c m :c {:a 1 :b :apples}))))
  (is (not (false? (c m :d [1 2] :c {:b :hi :a -100 :c {:h :hello}}))))
  (is (false? (c m :c {:a 1.0 :b :a})))
  (is (false? (c m :c {:a 1})))
  (is (false? (c m :c {:a 1 :c :hi})))
  (is (false? (c m :c {:a 1 :b 1})))
  (is (pred m [(pred> [#{(< 10) :i} a :k b -z -v [e f g]]) c]))
  (is (not (false? (c m :c {:a 1 :b :h :z :h :v :hh :g :h}))))
  (is (not (false? (c m :c {:a 1 :b :apples}))))
  (is (not (false? (c m :d [1 2] :c {:b :hi :a -100 :c {:h :hello}}))))
  (is (false? (c m :c {:a 1.0 :b :a})))
  (is (false? (c m :c {:a 1 :b :h :z :h :v :hh :g 1})))
  (is (false? (c m :c {:a 1})))
  (is (false? (c m :c {:a 1 :c :hi})))
  (is (false? (c m :c {:a 1 :b 1}))))

(deftest traits
  (is (desc person [:str name :i age :k sex :n height]))
  (is (desc- tall [(> 2) height]))
  (is (desc tall-person [(and> person? tall?) tall-person-input]))
  (is (describe short-person [(person?) short-person-input (< 1) height]))
  (is (describe- tall-person-bobby [(tall-person?) tall-person-bobby-input (= "bobby") name]))
  (is (desc- child [(person?) child-input (< 27) age]))
  (is (desc short-child [(child?) short-child-input (< 0.8) height]))
  (is (not (false? (c tall-person? :name "andrew" :sex :m :age 95 :height 2.1))))
  (is (false? (c tall-person? :name "andrew" :sex :m :age 95 :height 2)))
  (is (false? (c tall-person-bobby? :name "bobby" :sex :m :age 7 :height 2)))
  (is (false? (c tall-person-bobby? :name "andrew" :sex :m :age 7 :height 3)))
  (is (false? (c tall-person-bobby? :name "bobby" :age 7 :height 3)))
  (is (not (false? (c tall-person-bobby? :name "bobby" :sex :m :age 7 :height 3))))
  (is (not (false? (c short-person? :name "bobby" :sex :m :age 7 :height 0.5))))
  (is (false? (c short-person? :name "bobby" :sex :m :age 7 :height 1.6)))
  (is (not (false? (short-child? {:name "alice" :age 15 :sex :f :height 0.5}))))
  (is (false? (short-child? {:name "alice" :age 15 :sex :f :height 1.5})))
  (is (false? (short-child? {:name "alice" :age 35 :sex :f :height 0.5})))
  (is (thrown? AssertionError (make-short-child {:name "andrew" :age 25 :sex :m})))
  (is (thrown? AssertionError (make-short-child {:name "andrew" :age 25 :height 1.5 :sex :m})))
  (is (def andrew (make-short-child {:name "andrew" :age 25 :height 0.5 :sex :m})))
  (is (not (false? (short-child? andrew))))
  (is (df make-tall ((tall-person?)) [(person?) make-tall-input]
          (assoc make-tall-input :height 3)))
  (is (df make-small (:m) [(person?) make-small-input]
          (assoc make-small-input :height 0.1)))
  (is (not (false? (tall-person? (make-tall andrew)))))
  (is (false? (tall-person? (make-small andrew)))))

(deftest collections
  (is (pred vex [(epcoll> #(pred> % [:i a b])) v]))
  (is (not (false? (c vex :v [{:a 1 :b 2} {:c 5 :a 99 :b -20}]))))
  (is (pred vex2 [(epcoll> (predfn [:i a b]) map?) v]))
  (is (not (false? (c vex2 :v [{:a 1 :b 2} {:c 5 :a 99 :b -20}]))))
  (is (pred multkey [(epcoll> (predfn [a b]) (predfn [c])) i]))
  (is (false? (c multkey :i [{:a 1 :b 3}])))
  (is (not (false? (c multkey :i [{:a 1 :b 3 :c 4}]))))
  (is (not (false? (c multkey :i [{:a 1 :b 3 :c 1} {:a :h :b :h :c :h}]))))
  (is (false? (c vex2 :v [5 []])))
  (is (false? (c vex :v [{:a 1 :b 2} {:c 5 :a 99 :b -20.0}])))
  (is (false? (c vex :v [{:a 1} {:c 5 :a 99 :b -20}])))
  (is (pred vex [(epcoll> #(pred> % [:i a b -c])) v w -z]))
  (is (not (false? (c vex :v [{:a 1 :b 2} {:c 5 :a 99 :b -20}]
                      :w [{:a 1 :b 2} {:c 5 :a 99 :b -20}]))))
  (is (not (false? (c vex :v [{:a 1 :b 2} {:c 5 :a 99 :b -20}]
                      :w [{:a 1 :b 2} {:c 5 :a 99 :b -20}]
                      :z [{:a -1 :b -1}]))))
  (is (not (false? (c vex :v [{:a 1 :b 2 :c 100} {:c 5 :a 99 :b -20}]
                      :w [{:a 1 :b 2} {:c 5 :a 99 :b -20}]))))
  (is (not (false? (c vex :v [{:a 1 :b 2} {:a 99 :b -20}]
                      :w [{:a 1 :b 2} {:c 5 :a 99 :b -20}]
                      :z [{:a -1 :b -1}]))))
  (is (false? (c vex :v [{:a 1 :b 2} {:a 99 :b -20}]
                 :w [{:a 1 :b 2} {:c 1.1 :a 99 :b -20}]
                 :z [{:a -1 :b -1}])))
  (is (false? (c vex :v [{:a 1 :b 2} {:c 5 :a 99 :b -20.0}])))
  (is (false? (c vex :v [{:a 1} {:c 5 :a 99 :b -20}]
                 :w [{:a 1 :b 2} {:c 5 :a 99 :b -20}])))
  (is (false? (c vex :v [{:a 1 :b 2} {:c 5 :a 99 :b -20}] :z 5
                 :w [{:a 1 :b 2} {:c 5 :a 99 :b -20}])))
  (is (false? (c vex :v [{:a 1 :b -1} {:c 5 :a 99 :b -20}] :z [{:a 1 :b 1.1}]
                 :w [{:a 1 :b 2} {:c 5 :a 99 :b -20}]))))

(deftest defaults
  (is (desc dude [:i x [age (+ 10 15)]]))
  (is (desc dudes [(dude?) [guy (danger dude) guy2 (danger dude)]]))
  (is (desc dudes-easy [[guy (danger dude) guy2 (danger dude)]]))
  (is (= {:guy {:age 25} :guy2 {:age 25}}) (danger dudes))
  (is (describe dudes2 [[guy (danger dude) guy2 (danger dude)] :k cool-factor]))
  (is (thrown? AssertionError (c make-dudes2 :a 57)))
  (is (thrown? AssertionError (make-dudes {:guy {:age 25} :guy2 {:age 25}})))
  (is (= {:guy {:age 25} :guy2 {:age 25}} (make-dudes-easy {})))
  (is (= {:guy {:age 25} :guy2 {:age 25}} (make-dudes-easy {:guy {:age 25} :guy2 {:age 25}})))
  (is (= {:guy {:age 25} :guy2 {:age 20}} (make-dudes-easy {:guy {:age 25} :guy2 {:age 20}})))
  (is (= {:guy {:age 25} :guy2 {:age 20}} (make-dudes-easy {:guy2 {:age 20}})))
  (is (= {:guy {:age 25} :guy2 {:a 1}} (make-dudes-easy {:guy2 {:a 1}})))
  (is (= {:guy {:age 25} :guy2 {:age 25}} (make-dudes-easy {:guy2 (danger dude)})))
  (is (false? (dudes2? {})))
  (is (not (false? (dudes2? {:cool-factor :hi}))))
  (is (not (false? (dudes2? {:cool-factor :amazing :guy {:age 20} :guy2 {}})))))

;; clojure.test, a macro-based library, has some difficulty testing other complex macros
;; without use of eval. Perhaps this is because the blend macro uses eval,
;; unlike all other macros in Eat Static.
(deftest blended
  (is (desc ab [:i [a 5 b 6]]))
  (is (desc cd [:f [c d 1.1]]))
  (is (blend cdab [(ab?) [g (make-ab {:p :any})]] ab cd))
  (is (= {:a 5 :b 6 :c 1.1 :d 1.1 :g {:p :any :a 5 :b 6}} (eval '(make-cdab {}))))
  (is (= {:a 5 :b 6} (make-ab {})))
  (is (blend abcd [:k [cool :great]] cd ab))
  (is (= {:a 5 :b 6 :c 1.1 :d 1.1 :cool :great} (eval '(d abcd))))
  (is (= {:a 5 :b 6 :c 1.1 :d 1.1 :cool :great} (eval '(make-abcd {}))))
  (is (= (eval '(make-abcd {})) (eval '(d abcd))))
  (is (blend abcdr [:i q :k [cool :great]] cd ab))
  (is (thrown? AssertionError (eval '(make-abcdr {}))))
  (is (= [{:a 5 :b 6}{:a 5 :b 6}{:a 5 :b 6}] (dv ab 3)))
  (is (= (eval '(dv cdab 2)) [{:g {:p :any, :a 5, :b 6}, :d 1.1, :c 1.1, :a 5, :b 6}
                              {:g {:p :any, :a 5, :b 6}, :d 1.1, :c 1.1, :a 5, :b 6}]))
  (is (= (eval '(vmake cdab {:yo :man} 2)) [{:yo :man :g {:p :any, :a 5, :b 6}, :d 1.1, :c 1.1, :a 5, :b 6}
                                            {:g {:p :any, :a 5, :b 6} :yo :man, :d 1.1, :c 1.1, :a 5, :b 6}])))

(deftest describe-options
  (is (describe person [:i age]))
  (is (desc child [(person?) child-input (< 20) age]))
  (is (c child? :age 2))
  (is (false? (c child? :ags 4)))
  (is (= {:age 3} (make-child {:age 3})))
  (is (thrown? AssertionError (make-child {:age 30})))
  (is (thrown? AssertionError (make-child {:ages 2})))
  (is (describe baby-child [(child?) baby-child-input :b in-diapers] "new-" "??"))
  (is (describe baby-child2 [(child?) baby-child2-input :b in-diapers] "n-" "??"))
  (is (describe baby-child3 [(child?) baby-child3-input :b in-diapers :any [h 1 k :hi j "yo" l {:a 1}]] "n-" ""))
  (is (thrown? AssertionError (new-baby-child {:age 3})))
  (is (thrown? AssertionError (new-baby-child {:age 3 :in-diapers :true})))
  (is (= {:in-diapers true :age 3} (new-baby-child {:age 3 :in-diapers true})))
  (is (false? (baby-child?? {:age 100 :in-diapers true})))
  (is (baby-child?? {:age 1 :in-diapers false}))
  (is (baby-child2?? (n-baby-child2 {:age 1 :in-diapers false})))
  (is (baby-child3 (n-baby-child3 {:age 1 :in-diapers false})))
  (is (= {:age 2 :in-diapers true :h 1 :k :hi :j "yo" :l {:a 1}}
         (n-baby-child3 {:age 2 :in-diapers true}))))

(deftest default-settings-maps ;; aka final field
  (is (desc deff [{x 6 l :red}]))
  (is (blend deffer [{x 7}] deff))
  (is (blend deffer2 [[l :blue]] deff))
  (is (thrown? AssertionError (eval '(d deffer2))))
  (is (thrown? AssertionError (eval '(make-deffer2 {:l :blue}))))
  (is (= {:x 6 :l :red} (eval '(make-deffer2 {:l :red}))))
  (is (thrown? AssertionError (eval '(d deffer))))
  (is (thrown? AssertionError (eval '(make-deffer {:x 7}))))
  (is (thrown? AssertionError (eval '(make-deffer {:x 6}))))
  (is (false? (deff? {:l :blue :x 2})))
  (is (false? (deff? {:l :blue})))
  (is (= (make-deff (merge (d deff) {:h 1})) {:h 1 :l :red :x 6}))
  (is (= (make-deff {:h 1}) {:h 1 :l :red :x 6}))
  (is (deff? (d deff)))
  (is (desc tree [:i x {d deff}]))
  (is (= (danger tree) {:d {:x 6, :l :red}} ))
  (is (false? (tree? (danger tree))))
  (is (thrown? AssertionError (make-tree {})))
  (is (thrown? AssertionError (make-tree {:x 1.1})))
  (is (= {:x 33 :d {:x 6 :l :red}} (make-tree {:x 33 :d (d deff)})))
  (is (thrown? AssertionError (make-tree {:x 1 :d {:x 7 :l :red}})))
  (is (= {:x 33 :d {:x 6 :l :red}} (make-tree {:x 33 :d {:x 6 :l :red}})))
  (is (= {:x 33 :d {:aa :yo :x 6 :l :red}} (make-tree {:x 33 :d {:aa :yo :x 6 :l :red}})))
  (is (tree? {:x 0 :d (d deff)}))
  (is (tree? {:x 22}))
  (is (df j [x [y 8]] [x y j-input]))
  (is (= (j {:x 5}) [5 8 {:x 5}])))

(deftest blended-overrides
  (is (desc a9 [[a 9]]))
  (is (blend a9b8 [[b 8]] a9))
  (is (eval '(blend anil [[a nil]] a9b8)))
  (is (= {:a 9 :b 8} (eval '(d anil))))
  (is (= {:a 9 :b 8} (eval '(d a9b8))))
  (is (eval '(blend aRb8 [a] a9b8)))
  (is (eval '(blend anil2 [[a nil]] aRb8)))
  (is (thrown? AssertionError (eval '(d aRb8))))
  (is (thrown? AssertionError (eval '(d aRb8))))
  (is (thrown? AssertionError (eval '(make aRb8 {}))))
  (is (thrown? AssertionError (eval '(mc aRb8 :b 99))))
  (is (= {:a 44 :h 77 :b 8} (eval '(mc aRb8 :a 44 :h 77))))
  (is (eval '(blend a1b8 [[a 1]] aRb8)))
  (is (= {:a 1 :b 8} (eval '(d a1b8))))
  (is (eval '(blend aint [:i -a] a1b8)))
  (is (eval '(blend aint2 [:i -a] aRb8)))
  (is (= {:a 1 :b 8} (eval '(d aint))))
  (is (= {:a 99 :b 8} (eval '(mc aint :a 99))))
  (is (= {:a 123 :b 8} (eval '(mc aint2 :a 123))))
  (is (eval '(blend aintd [[a -22]] aint)))
  (is (= {:a -22 :b 8 :y "hi there"} (eval '(mc aintd :y "hi there"))))
  (is (not (false? (eval '(aintd? {})))))
  (is (not (false? (eval '(aintd? {:a -22})))))
  (is (not (false? (eval '(aintd? {:a 1000})))))
  (is (false? (eval '(aintd? {:a :hi}))))
  (is (false? (eval '(aintd? {:a 1.1}))))
  (is (false? (eval '(c aintd? :a :hi))))
  (is (thrown? AssertionError (eval '(d aint2))))
  (is (thrown? AssertionError (eval '(mc aint2 :a 123.1))))
  (is (thrown? AssertionError (eval '(mc aint :a 123.1))))
  (is (eval '(blend aop [-a] aRb8)))
  (is (thrown? AssertionError (eval '(d aop))))
  (is (= {:a :hi :b 8} (eval '(mc aRb8 :a :hi))))
  (is (eval '(blend b8 [[a 9]] a1b8)))
  (is (eval '(blend b8b [-a [b 8]] a9)))
  (is (eval '(blend b8c [-a] a9b8)))
  (is (= (eval '(d b8c)) (eval '(d b8)) (eval '(d b8b))))
  (is (eval '(blend b7final [{b 7}] a1b8)))
  (is (= {:a 1 :b 7} (eval '(d b7final))))
  (is (eval '(blend b99 [[b 99]] b7final)))
  (is (thrown? AssertionError (eval '(d b99))))
  (is (= {:a 1 :b 7 :jj :hello} (eval '(make b99 {:b 7 :jj :hello}))))
  (is (eval '(blend nob [-b] b7final)))
  (is (= (eval '(d nob)) (eval '(d b7final))))
  (is (eval '(blend a12final [:f {a 12}] a9)))
  (is (eval '(blend a12final2 [{a 13}] a12final)))
  (is (eval '(blend a12final3 [{a 12}] a12final2)))
  (is (eval '(blend a1fail [[a 1]] a12final)))
  (is (eval '(blend a2intfinal [{a 2}] aint)))
  (is (eval '(blend a2intfinal2 [{a 2}] aint2)))
  (is (eval '(blend ahiddenfinal [] a2intfinal2)))
  (is (eval '(blend a2intfinal2fail [{a 2.2}] aint2)))
  (is (= {:a 12} (eval '(d a12final))))
  (is (= {:a 12} (eval '(make a12final {:a 12}))))
  (is (= {:a 12 :y 87} (eval '(make a12final {:a 12 :y 87}))))
  (is (= {:a 12 :y 87} (eval '(make a12final {:y 87}))))
  (is (thrown? AssertionError (eval '(make a12final {:a 11}))))
  (is (thrown? AssertionError (eval '(d a1fail))))
  (is (thrown? AssertionError (eval '(d a12final2))))
  (is (thrown? AssertionError (eval '(mc a12final2 :a 13))))
  (is (thrown? AssertionError (eval '(mc a12final2 :a 12))))
  (is (thrown? AssertionError (eval '(d a12final3))))
  (is (thrown? AssertionError (eval '(mc a12final3 :a 13))))
  (is (thrown? AssertionError (eval '(mc a12final3 :a 12))))
  (is (= {:a 12} (eval '(mc a1fail :a 12))))
  (is (= {:b 8 :a 2} (eval '(d a2intfinal))))
  (is (= {:b 8 :a 2} (eval '(d ahiddenfinal))))
  (is (thrown? AssertionError (eval '(mc ahiddenfinal :a 1))))
  (is (thrown? AssertionError (eval '(mc a2intfinal2 :a 1))))
  (is (= {:b 8 :a 2} (eval '(d a2intfinal2))))
  (is (= {:hh 88 :b 8 :a 2} (eval '(mc a2intfinal :hh 88))))
  (is (thrown? AssertionError (eval '(d a2intfinal2fail))))
  (is (thrown? AssertionError (eval '(mc a2intfinal2fail :a 2)))))

(deftest all-same-defaults
  (is (desc y6a [:i [y 6 x] :f -w -t]))
  (is (blend y6 [[y 6]] y6a))
  (is (eval '(blend noy [-y] y6)))
  (is (eval '(blend noy2 [[y]] y6)))
  (is (eval '(blend noy3 [[y]] noy2)))
  (is (eval '(blend noy4 [-y] noy3)))
  (is (eval '(blend noy5 [-y] noy)))
  (is (eval '(blend noy6 [-y] y6a)))
  (is (= {:y 6} (d y6a) (eval '(make y6 {}))
         (eval '(d noy)) (eval '(d noy2)) (eval '(d noy3))
         (eval '(d noy4)) (eval '(d noy5)) (eval '(d noy6))))
  (is (thrown? AssertionError (eval '(mc noy6 :x 3.3))))
  (is (= {:x 55 :y 6} (eval '(mc noy6 :x 55)) (eval '(make noy6 {:x 55}))))
  (is (thrown? AssertionError (eval '(mc noy6 :w 1))))
  (is (= {:w 55.1 :y 6} (eval '(mc noy6 :w 55.1)) (eval '(make noy6 {:w 55.1}))))
  (is (eval '(blend conflict [:i -w] noy6)))
  (is (thrown? AssertionError (eval '(make noy6 {:w 2}))))
  (is (thrown? AssertionError (eval '(make noy6 {:x 2.1}))))
  (is (eval '(blend req [w] noy3)))
  (is (false? (eval '(c req? :w 12 :y 6))))
  (is (= {:w 1.1 :y 78} (eval '(mc noy6 :w 1.1 :y 78)))))
