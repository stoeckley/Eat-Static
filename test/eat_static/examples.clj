;;  Copyright (c) Andrew Stoeckley, 2015. All rights reserved.

;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the license file at the root directory of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns eat-static.examples
  (:require [eat-static.validations :refer :all]))

;; Examples for the main Readme page of this library. See the Readme for
;; explanations of this code.

(df my-function [a b c] )

;; instead of:

(defn my-function [{:keys [a b c] :as my-function-input}] )


;; Additionally, the df version makes the keys a,b,c required, 
;; unlike the simple defn version which sets them to nil if they are omitted.
;; Read on to see how easy it is to change these requirements.

(my-function {:a 1 :b 2 :c 4})

(c my-function :a 1 :b 2 :c 4)

(c my-function :c 4 :a 1 :b 2) ; same as above

(df my-function [[a b c 0]] )

;; instead of:

(defn my-function [{:keys [a b c] :or {a 0 b 0 c 0}
                    :as my-function-input}] )

(df my-function [[a b c 0]] )

;; Same as above, no change necessary.
;; The map is already implicitly available in the body.
;; (More details below).

;; instead of:

(defn my-function [{:keys [a b c] :or {a 0 b 0 c 0}
                    :as my-function-input}] ) ;; added :as local

(df my-function [:int [a b c 0]] )

;; Several shortcuts are offered as well:

(df my-function [:i [a b c 0]] )

;; This causes an exception if you pass in non-integer values for a,b,c.

;; Equivalent:

(defn my-function [{:keys [a b c] :or {a 0 b 0 c 0}
                    :as my-function-input}]
                   {:pre [(integer? a) (integer? b) (integer? c)]}
                    )

;; In reality, the df macro is implemented as assert calls, 
;; not a :pre/:post map, to offer specific error messages
;; and to handle other features described below.

(df my-function [a [b c 0]] )
;; a is required, but b,c are optional, with same default value

(df my-function [a [b c]] )
;; b,c are optional, no default value (remain nil if omitted)

(df my-function [:i a [b 5 c 6 d 7] e :n f] )
;; a,e,f are required and b,c,d each have a different default value
;; a,b,c,d,e must be integers, but f can be any number
;; (more details below)

;; Preview of the validation expressions described below:

(df two-ints [(> b) a :i b (< 10) a b] )

;; b must be an integer and a must be greater than b, 
;; but both a and b must be less than 10.
;; if any of these fail, the whole function call fails

;; By default, the whole map is available as a symbol derived from
;; the function name. If the function is my-function, the map can 
;; be accessed in the body as my-function-input.

;; An additional parameter before the arg vector provides a custom name 
;; for the whole map that was passed in, rather than using the default 
;; name implicitly provided:

(df my-function input-map 
 [a b c]
 ;; do something with input-map...
 )

;; One particularly interesting feature is that you can reference this
;; whole input map in the arg list as well, and treat it with overall validation.
;; This has useful consequences when composing predicates and working with custom
;; types and traits. (see further below).
;; However, you cannot specify the whole input map as optional, of course.

;; Support for doc strings, of course:

(df my-function
 "A fantastic doc string"
 [a b c]
 
 )

;; Both a doc string and a custom name for the entire input map:

(df my-function 
 "A really great doc string"
 input-map 
 [a b c]
 )

;; The optional parameters of a custom map name and a doc string
;; can actually appear in any order:

(df my-function 
 input-map 
 "Still a really great doc string"
 [a b c]
 )

(df validate-output
    (:i)
    [a]
    a)

;; In this example, the return of validate-output must be an integer, 
;; or a run-time assertion triggers.

;; A list that appears anywhere before the argument vector 
;; follows the same syntax as the arg vector, but in list form.

(df another
    "Output must be an integer less than 10"
    custom-input-map-name
    (:i (< 10)) ; validation expressions like this are explained below
    [:i a b]
    (+ a b))

;; The doc string, custom input map name and output validation list
;; are all optional, and may appear in any order:

(df another
    (:i (< 10))
    custom-input-map-name
    "Output must be an integer less than 10"
    [:i a b]
    (+ a b))

;; If your return value was a map, you could use the validation tools explained
;; below to validate the individual elements of the output map as well. 

;; For example, this expression requires the output of the function to be 
;; a map containing non-nil values for keys :foo and :bar --

(df has-keys-foo-bar
   ((:foo)(:bar))
   [a] )

;; same as this use of the pred> form, explained further below:

(df has-keys-foo-bar
   ((pred> [foo bar]))
   [a] )

;; Requires :foo and :bar in the output map to be integers:

(df has-keys-foo-bar
   ((pred> [:i foo bar]))
   [a] )

;; This requires the output to be either 0 or 1, and nothing else:

(df zero-or-one
    ((or> #(= 1 %) #(= 0 %)))
    [x] )

;; pred> and or> and other useful validation helpers and described further below

;; This requires the output to be an integer greater than 10
;; OR less than -5:

(df out (:i (or> #(> % 10) #(< % -5)))
    [:i x] )

;; For comparison's sake, here is the normal Clojure way
;; to do almost the same thing:

(defn out
      [{:keys [x] :as out-input}]
      {:pre [(integer? x)]
       :post [(integer? %)
              (or (> % 10) (< % -5))]}
      :some-output)

;; (Eat Static does not use the :pre/:post map internally)

(defn circle
  [{:keys [radius x y color] :or {color :blue}
    :as input}]
  {:pre [(>= radius 1) (integer? x) (integer? y)
         (#{:blue :white} color)]}
  :some-output
  )

(df circle
  [(>= 1) radius :i x y (#{:blue :white}) [color :blue]]

  )

(defn circle
  [{:keys [radius x y z color] :or {color :blue} ;;added z
  :as input}]
  {:pre [(>= radius 1) (integer? x) (integer? y)
         (#{:blue :white} color)
         (if z (integer? z) true)]} ;;added this line
  :some-output
  )

(df circle
   [(>= 1) radius :i x y -z (#{:valid :colors}) [color :blue]]
  )

;; added -z

(df circle
    [(>= 1) radius (neg?) x y])

;; in this example, it is not necessary to
;; specify that these are numbers since the
;; validation checks that for us

;; The same arg can be used in multiple checks:

(df circle
    [(>= 1) radius :i radius x y])

;; x y and radius must be integers
;; and radius must be at least 1

;; Note: the keyword validators for primitives
;; such as :i, :n and many others are only available
;; in the top level of an argument vector, and not
;; inside validation forms like (>= 1) which may
;; use keywords for other purposes.

(df function
    [(++ :i (> 1)) x y z])

;; x, y, z must all be integers greater than 1

;; Note: You may have noticed earlier that output validation lists
;; automatically require all expressions to pass for the single return
;; value, therefore they operate like ++. Thus, ++ is unnecessary and
;; not available for output validation lists.

(df function
    [(or> #(< 2 % 10) #(> -2 % -10)) x y])

;; x and y must both be between 2 and 10 or between -2 and -10

;; and and> would simply require that all functions are true.

;; Any validiation expression can be combined with ++ :

(df int-stuff
    (:i (< 1))
    [(++ :i (or> #(< 2 % 10) #(> -2 % -10))) [x y 5]]
)

;; x and y must also be integers between one of these two ranges

;; and> and or> mean the same thing if you just use a single
;; anonymous function expression

;; standard Clojure equivalent of int-stuff:

(defn int-stuff
      [{:keys [x y] :or {x 5 y 5}
        :as int-stuff-input}]
      {:pre [(integer? x) (integer? y)
             (or (< 2 x 10) (> -2 x -10))
             (or (< 2 y 10) (> -2 y -10))]
       :post [(integer? %) (< % 1)]}
      :some-output)

;; Create a simple fn that returns true if its args pass:

(df is-senior? [:string name (> 65) age]
  true)

;; Use it as your trait check:

(df process-seniors
  [(is-senior?) person1 person2]
)

;; Error if you call this fn with maps that don't exhibit the required trait

;; Much better way:

(pred is-senior? [:string name (> 65) age])

;; The fn is-senior? always returns truthiness or false, and performs no asserts.

;; You can still use this as a trait check, but you can also use it in
;; other places where a false return value is a valid possibility.

;; Predicates created with pred are not allowed to have a function body,
;; but they do accept doc strings and custom names for the input map.

((predfn [a b]) {:a 1})  ;this ad-hoc test returns false for the map {:a 1}

;; predfn accepts the arg list only -- no fn name, no doc string, no body.
;; You also cannot access the full input map with predfn.

;; You could inline it as a quick custom check 
;; for a function argument that is a map:

(df foo [((predfn [a b])) mymap] )

;; When using predfn as an ad-hoc test inside a validation expression like this,
;; you can simplify with the pred> macro, which removes a layer of parens:

(df foo [(pred> [a b]) mymap] )

;; This macro introduces a new layer of the special arg vector, and you can nest
;; these as deep as you need for complex object hierarchies, if necessary

;; But if you merely want to see if a single key exists in a map supplied as
;; an argument, this is easier:

(df foo [(:a) mymap])

;; expands to a check of (:a mymap)

;; the *> helpers are laid out to simplify validation expressions, 
;; and several are introduced below.

;; pred and predfn are not assertive, but this foo call is assertive
;; as all df forms enforce their arguments to the specified criteria.
;; foo fails if mymap does not have :a and :b keys

;; If you want to pass in a vector of persons and make sure it is a homogenous
;; collection that meets a trait requirement, you could write:

(df process-seniors
   [(#(every? is-senior? %)) persons]
   )

;; That's not too bad, but Eat Static provides another validation helper, epcoll>, 
;; which combines the Clojure core fns every-pred and coll? and also reverses
;; the order of arguments to make it more idiomatic for these checks.
;; It implicitly requires the argument to pass the coll? test *first* so you can
;; still get a false value rather than an exception in cases where you need that:

(pred is-awesome? [(= :super-cool) cool-factor])

(df process-awesome-seniors
    [(epcoll> is-senior? is-awesome?) persons]
    )

;; will fail unless persons is a collection and 
;; each item is is-senior? *and* is-awesome?

;; Or you could test that each map in a vector has the keys a and b:

(df all-have-a-b
    [(epcoll> (predfn [a b])) maps maps2]
    )

(c all-have-a-b :maps [{:a 1 :b 2} {:a 5 :b 6 :c 7}]
                :maps2 [{:a "hi" :b 11.1} {:b 6 :a :yo}])

;; these arguments pass validation

;; You could make a robust "person" object using a constructor built
;; with df that simply returns the map if the parameters passed all checks:

(df make-person
    [:bool eats-meat :str name [spouse] country :i age :k education]
    make-person-input)

(make-person {:name "Ludwig" :spouse "Angela" :education :college
              :country "USA" :age 39 :eats-meat false})

(make-person {:name "Bobby" :country "USA" :age 4
              :eats-meat true :education :pre-school})

;; Bobby doesn't have a spouse (yet)

;; Your map will fail if it doesn't meet the types and parameters
;; required by make-person

;; Worth noting that this same function can also be used as an assertive
;; predicate on maps already constructed, to verify they are the 
;; "make-person" type. But, typically, for true/false verification, it is better
;; to just build a pred:

;; Verifies a map is a "person" object:

(pred person? [eats-meat :str name [spouse] country :i age :k education])

;; Note that the df form and the pred form simply have the exact same
;; vector-formatted arg list. 
;; You can accomplish both at once using the describe macro:

;; Two birds, one stone:

(describe child [:str name fav-toy :i age :k education])

;; This one-liner generates an assertive constructer called make-child
;; and a true/false non-assertive validator called child?

(def alex
   (make-child {:name "alex" :fav-toy "legos"
                :age 8 :education :primary}))

;; (this is a contrived example; you probably wouldn't store alex in a def)

(child? alex) ;; returns a truthy value

;; In both functions, the input map is also named child-input should you
;; wish to use it in the argument vector to (describe ...), i.e. for 
;; creating new objects of existing objects:

(desc baby-child [(child?) baby-child-input (< 2) age])

;; optionally naming the two functions generated:

(describe baby-child [(child?) baby-child-input (< 2) age] new- "")


;; Additionally, describe- creates private versions of the constructor and validator

;; the ep> validation helper is explained a bit further below

(desc person [:str name :i age :k sex :n height])
(desc tall [(> 2) height])
(desc tall-person [(ep> person? tall?) tall-person-input])
(desc short-person [(person?) short-person-input (< 1) height])
(desc tall-person-bobby [(tall-person?) tall-person-bobby-input (= "bobby") name])
(desc child [(person?) child-input (< 27) age])
(desc short-child [(child?) short-child-input (< 0.8) height])

;; fails:

;(make-short-child {:name "andrew" :age 25 :height 1.5 :sex :m})

(pred is-dutch? [(= "netherlands") country])

(df invite-to-amsterdam-elder-poker-party
    [(is-senior?) person (is-dutch?) person] )

;; If instead of explicitly trusting these args and operating on them, 
;; you simply want to verify a combination of traits:

(pred is-dutch-senior? [(is-senior?) person (is-dutch?) person])

;; call it:

(def hank {})

(c is-dutch-senior? :person hank)

;; You get truth or false: Either Hank is a senior citizen who lives in Holland
;; or isn't. 

(desc person [:str name country :i age :k sex :n height :b eats-meat])

(pred american-meat-eating-child? [(< 10) age (= true) eats-meat (= "USA") country])

(american-meat-eating-child? {:age 50 :eats-meat true :country "USA"}) ;; false

(def jimmy (make-person {:age 5 :eats-meat true :country "USA"
                         :height 1 :sex :m :name "jimmy"}))

(american-meat-eating-child? jimmy) ;; truth

;; To make sure a young american cat doesn't pass the test:

(pred american-child-likes-meat? 
   [(person?) kid (american-meat-eating-child?) kid])

(c american-child-likes-meat? :kid jimmy)

;; Note the difference in the way these two are called:

(american-meat-eating-child? jimmy)

(american-child-likes-meat? {:kid jimmy})

;; This is because the second function used two predicates
;; on a single map passed as an arg, while the first tested 
;; the contents of the map directly. Accessing the input map
;; directly in the arg list helps us here:

(pred american-child-likes-meat? kid ;; added kid to name input map
   [(person?) kid (american-meat-eating-child?) kid])

;; All we did is make "kid" the name of the full input map instead.
;; Now you can pass the map directly.


(pred is-dutch? [(= "netherlands") country])

(pred is-senior? [(> 65) age])

(pred is-vegetarian? [(= false) eats-meat])

(pred elder-dutch-vegetarian? person
      [(person?) person
       (is-senior?) person
       (is-vegetarian?) person
       (is-dutch?) person])

;; With Clojure's every-pred, you can compose the predicates and thus
;; avoid repeating parameter names:

(pred elder-dutch-vegetarian? person
      [((every-pred person? is-senior? is-vegetarian? is-dutch?)) person])

;; Another helper ep> simplifies this:

(pred elder-dutch-vegetarian? person
      [(ep> person? is-senior? is-vegetarian? is-dutch?) person])

;; This is just like epcoll> except it doesn't take a collection.

;; If all your checks are on the same input map and none of its
;; individal elements, the above are more easily composed as follows:

(def old-dutch-veggie-eater? (every-pred person? is-senior? is-vegetarian? is-dutch?))

(def veggie-eater? (every-pred person? is-vegetarian?))

(def young-veggie-eater? (every-pred person? (complement is-senior?) is-vegetarian?))
;; or
(def young-veggie-eater? (every-pred person? (predfn [(< 20) age]) is-vegetarian?))
;; or
(pred young-veggie-eater? m [(ep> person? is-vegetarian?) m (< 20) age])

;; (pred ...) is typically easier when you combine
;; full-map validation with more complex individual element checks

;; Using our person? predicate from above, we can analyze interactions
;; between maps:

(defn married? 
   [husband wife]
   (and (person? husband) (person? wife)
        (= (:name husband) (:spouse wife))))  

;; If you want to make sure you never call married? with numbers or 
;; pets instead of people, it just becomes:

(df married? 
    [(person?) husband wife]
    (= (:name husband) (:spouse wife)))

;; Calling the df version is different than calling the defn version, of course,
;; since the parameters are now named by the caller, their ordering no longer matters,
;; and passing a person type is required.

;; For completeness' sake, here is the pred version, demonstrating that you can
;; access other arguments in a validator:

(pred married?
      [(person?) husband wife
       (#(= (:name husband) (:spouse %))) wife])    

;; You could technically write entire complex functions inside an
;; argument list, but that's not really the idea. And remember for pred, 
;; it only ever returns a truthy or false value.

;; Using married? and other quick predicates we built above:

(df old-dutch-vegetarian-spouses?
    [(person?) husband wife]
    (and (c married? :husband husband :wife wife)
         (elder-dutch-vegetarian? husband)
         (elder-dutch-vegetarian? wife)))

;; And a slightly more succinct version, for example's sake:

(df old-dutch-vegetarian-spouses? in
    [(person?) husband wife]
    (and (married? in)
     ((predfn [(elder-dutch-vegetarian?) husband wife]) in)))

;; calling it:

;; (old-dutch-vegetarian-spouses? {:husband ludwig :wife allison})

;; or

;; (c old-dutch-vegetarian-spouses? :wife alice :husband jason)


;; This function returns true or false as long as we pass in person
;; objects. If we lifted the elder-dutch-vegetarian? checks into the
;; arg list as well, it would *require* them to be true, which is not
;; the obvious goal of this function. But read on...

;; Alternative:

(pred old-dutch-vegetarian-spouses?
      [(elder-dutch-vegetarian?) husband wife 
       (c> married? :husband :wife wife) husband])

;; The value of husband is associated with the name :husband

;; This version is non-assertive regardless of inputs. It removes the person?
;; check since elder-dutch-vegetarian? already checks that. 

;; Of course, unordered named parameters let you do it this way also:

(pred old-dutch-vegetarian-spouses?
      [(elder-dutch-vegetarian?) husband wife 
       (c> married? :wife :husband husband) wife])

(pred old-dutch-vegetarian-spouses? in
      [(elder-dutch-vegetarian?) husband wife 
       (married?) in])

;; You call these the same as the above df version:

;; (c old-dutch-vegetarian-spouses? :wife alice :husband jason)

;; these two expressions are the same, and return false:

((predfn [a b]) {:a 1})

(pred> {:a 1} [a b])

(c my-function :c 4 :a 1 :b 2 :e 5 :f 6)

;; this provides interesting uses for trait-like behavior, 
;; describe further below

;; this tests that the vector passed in contains only integers:

(df intvec [(epcoll> integer?) v]
    )

(df intvec [(++ :v (epcoll> (t :i))) v]
    v)

(df intvec [(epv> (t :i)) v]
    v)

(df intvecl [(epl> (t :i)) v]
    v)

;; enforces that the collection passed in contains only integers
;; or keywords:

(df vari [(or> #(epcoll> % (t :i))       
               #(epcoll> % (t :k)))
          v]
    v)


;; lifting the type out as its own function:
(defn vec-of-ints [x] (epv> x (t :i)))
(df many-vecs [(vec-of-ints) a b c] [a b c])


(pred kitty [:k color :i age :b likes-milk])

(defn bunch-o-kitties [ks] (epv> ks kitty))

(df feed-kitties [(bunch-o-kitties) cats] cats )

(def sexes #{:male :female})

(df person [:str name (sexes) sex] person-input)

;; The function will only accept a value for "sex" that is :male or :female


(describe defaults [:i a [b 8 c 9]])

(make-defaults {:a 1})

;; returns {:a 1 :b 8 :c 9}

(make-defaults {:a 1 :b 2})

;; returns {:a 1 :b 2 :c 9}

;; a and b have default values, and q is required:
(describe ab [:i q [a 1 b 2]])
;; all must be integers

;; c and d have default values and w is required
(describe cd [:f w -r :any [c 3 d 4]])
;; w must be a float, while c and d can by anything

;; e is a required keyword, and ab-cd is both a ab and a cd type
(blend ab-cd [:k e] ab cd)

;;these will fail because required values of the composite types are omitted:

;;(make-ab-cd {:e :hi})
;;(make-ab-cd {:e :hi :w 1.1})

;; but this passes:

(make-ab-cd {:e :hi :w 1.1 :q 55})

;; returned:
;; {:e :hi, :w 1.1, :q 55, :a 1, :c 3, :b 2, :d 4}

(desc red-rectangle [:n [width 5 height 3] :k [color :red]])

(desc square [:n [width 5 height 5]])

;; makes a red square from a red rectangle
(blend red-square [] red-rectangle square)

(make-red-square {})

;;returns {:width 5, :height 5, :color :red}

;; however, if the order to blend is reversed:
(blend red-square [] square red-rectangle)

(make-red-square {})

;; returns {:color :red, :width 5, :height 3}
;; not a square! because the final blended item was a rectangle,
;; which gets priority.

;; of course, you can make a blue square too:
(blend blue-square [[color :blue]] red-rectangle square)

;; returns {:width 5, :height 5, :color :blue}

(desc baby-white-kitty [:k [color :white] :i [age 0] :b [likes-milk true]])

(d baby-white-kitty)

;; returns {:color :white, :age 0, :likes-milk true}

(def feline-litter (dv baby-white-kitty 5))

;; contains 5 baby white kitties

[{:color :white, :age 0, :likes-milk true}
 {:color :white, :age 0, :likes-milk true}
 {:color :white, :age 0, :likes-milk true}
 {:color :white, :age 0, :likes-milk true}
 {:color :white, :age 0, :likes-milk true}]

(desc car [:i [age 0] :str [make "GM"]])

(desc new-car-purchase [:str store (car?) [new-car (d car)]])

(c make-new-car-purchase :store "Al's Car Shop")

;; returns {:store "Al's Car Shop", :new-car {:age 0, :make "GM"}}

(desc factory-output [(epv> car?) [cars (dv car 5)]])

(make-factory-output {})

;; returns: 

{:cars
 [{:age 0, :make "GM"}
  {:age 0, :make "GM"}
  {:age 0, :make "GM"}
  {:age 0, :make "GM"}
  {:age 0, :make "GM"}]}


(set-describe-names! "front" "back")

(desc fb [:k q w e [r :whatever]])

(make fb {:q :ui :w :w :e :e})
;; allows you to forget the naming scheme that is set

;;(is? fb *1)

(d fb)
;; returns {:r :whatever}
;; getting defaults is described further below

;; these two functions return unresolved symbol errors, 
;; since the naming scheme has changed:

;;make-fb
;;fb?

;; however these two now exist instead:

frontfb
fbback
