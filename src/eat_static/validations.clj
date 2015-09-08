;;  Copyright (c) Andrew Stoeckley, 2015. All rights reserved.

;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the license file at the root directory of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns ^{:author "Andrew Stoeckley at Balcony Studio, the Netherlands"
      :doc "Eat Static is a library for the quick validation of Clojure maps and functions, offering easy safety checks for input, output and rapid generation of custom types and traits. See the main Github page or the Readme.md file in the root of this distribution for comprehensive walkthrough of the library's usage and features."}
  eat-static.validations)

(defn is-bool?
  "Determines if the provided value is a true or false value."
  [x]
  (contains? #{true false} x))

;; clojure primitive type checks
;; including quotations for use in macros
(def types
  {:boolean ['is-bool? is-bool?]
   :fn ['fn? fn?] 
   :symbol ['symbol? symbol?] 
   :keyword ['keyword? keyword?]
   :string ['string? string?]
   :char ['char? char?]
   :identity ['identity identity]
   :map ['map? map?]
   :vector ['vector? vector?]
   :set ['set? set?]
   :list ['list? list?]
   :number ['number? number?]
   :ratio ['ratio? ratio?]
   :float ['float? float?]
   :integer ['integer? integer?]})

(def type-checks
  {:bool (:boolean types)
   :boolean (:boolean types)
   :b (:boolean types)
   :fn (:fn types)
   :sym (:symbol types)
   :symbol (:symbol types)
   :k (:keyword types)
   :key (:keyword types)
   :keyword (:keyword types)
   :str (:string types)
   :string (:string types)
   :c (:char types)
   :char (:char types)
   :anything (:identity types)
   :any (:identity types)
   :identity (:identity types)
   :m (:map types)
   :map (:map types)
   :v (:vector types)
   :vec (:vector types)
   :vector (:vector types)
   :set (:set types)
   :l (:list types)
   :list (:list types)
   :n (:number types)
   :num (:number types)
   :number (:number types)
   :r (:ratio types)
   :rat (:ratio types)
   :ratio (:ratio types)
   :f (:float types)
   :float (:float types)
   :i (:integer types)
   :int (:integer types)
   :integer (:integer types)})

(defn throw-text [s]
  (throw (Exception. s)))

(defn- is-pre-or-post
  "Determines if the first form provided in a function body is a :pre/:post map."
  [m]
  (and (map? m)
       (clojure.set/subset? (set (keys m)) #{:pre :post})))

(defn- is-optional
  "Determines if a symbol naming a local is preceded by a hyphen, indicating it is optional."
  [x]
  (= \- (first (str x))))

(defn- symbol-root
  "Returns the raw symbol, regardless of whether it is indicated as optional or not."
  [x]
  (if (is-optional x)
    (symbol (apply str (rest (str x))))
    x))

(defn- place-symbol
  "Places the symbol in various contexts, such as whether it is a required or optional function argument, and any type checks and/or other tests it must pass when the function is called."
  [ass place sym input-map return & {:keys [process remain]}]
  (let [l (:lastfn ass)
        put #(cond
               (= sym input-map) (update-in % [%2] conj sym)

               (= sym return) (throw-text "wrong use of place-symbol")

               ;; (and (= :opt place) (some #{sym} (:opt ass)))
               ;; (throw-text (str "Multiple declarations of optional value " sym))

               (or (and (= :opt place) (some #{sym} (:req ass)))
                   (and (= :req place) (some #{sym} (:opt ass))))
               (throw-text (str "Symbol " sym " cannot be both optional and required."))
               
               :else (-> % (update-in [place] conj sym)
                         (update-in [%2] conj sym)))]
    (cond (= :finished process) ass
          process (-> ass (put process)
                      (place-symbol place sym input-map return
                                    :process (or (first remain) :finished)
                                    :remain (next remain)))
          (vector? l) (place-symbol ass place sym input-map return
                                    :process (first l)
                                    :remain (next l))
          :else (put ass l))))

(defn- arg-split-fn
  "This is the reduction function used when looking at all the forms provided in a function definition's argument vector, or an output validation list. It places symbols into their required contexts."
  [input-map is-output? return]
  (fn [result arg]
    (cond
      (and (list? arg) (= '++ (first arg)))
      (do (assert (not is-output?) "A function's output validation list already acts like ++ and thus ++ should not be used.")
          (assert (every? (fn [x] (or (keyword? x) (list? x))) (rest arg)) "++ only accepts keywords or lists as validators.")
          (assert (< 1 (count (rest arg)))
                  "++ only makes sense with at least two validators after it. Otherwise, use the validator directly in isolation.")
          (if is-output?
            (update-in result [:output-validations] conj (vec (rest arg)))
            (assoc result :lastfn (vec (rest arg)))))

      (or (list? arg) (keyword? arg))
      (if is-output?
        (update-in result [:output-validations] conj arg)
        (assoc result :lastfn arg))

      (symbol? arg)
      (let [sym (symbol-root arg)]
        (cond
          is-output?
          (throw-text "Output validation lists always operate on the function's return value, and no other symbols may be named.")
          
          (and (is-optional arg) (or (= input-map sym) (= return sym)))
          (throw-text "Full input map or output return value cannot be optional.")

          (= return sym)
          (let [l (:lastfn result)]
            (if (vector? l)
              (reduce #(update-in % [:output-validations] conj %2)
                      result l)
              (update-in result [:output-validations] conj l)))
          
          :else (place-symbol result (if (is-optional arg) :opt :req) sym input-map return)))

      (vector? arg)
      (if is-output?
        (throw-text
         "Output validations always operate on the function's return value, and no other symbols or default vectors may be named.")
        (let [vs (reverse arg)
              assigns (loop [v nil process vs r {}]
                        (if (seq process)
                          (if (not (symbol? (first process)))
                            (recur (first process) (rest process) r)
                            (if (#{input-map return} (first process))
                              (throw-text "Full input map or output value cannot be optional.")
                              (recur v (rest process)
                                     (assoc r (first process) v))))
                          r))]
          (reduce (fn [r [s default]]
                    (-> r (place-symbol :opt (symbol-root s) input-map return)
                        (update-in [:defaults] conj [(symbol-root s) default])))
                  result assigns)))

      :else (if is-output?
              (throw-text "Output validation list list contained an element that is not a keyword or list.")
              (throw-text "Arg list contained an element that is not a symbol, vector, keyword, or list.")))))

(defn- arg-split
  "Splits and analyzes the vector of function arguments or the list of output validations."
  [args input-map is-output? return]
  (dissoc
   (reduce (arg-split-fn input-map is-output? return)
           {:lastfn :no-fn :opt #{} :req #{} :output-validations []}
           args)
   :lastfn))

(defn- assert-sym
  "Creates the assertion for a single test for a single symbol. Multiple calls will assert the same symbol for multiple different tests. Creates either an assertion statement or a simple function call, depending on whether the function is assertive or a true/false predicate function, non-assertive."
  [f s t is-pred?]
  (if (keyword? f)
    (if-let [kfn (first (get type-checks f))]
      (if is-pred?
        `(~kfn ~s)
        `(assert (~kfn ~s) ~(str t ": Type " kfn " for " s)))
      (throw-text (str "Invalid type keyword in argument list: " f)))
    (if is-pred?
      `~(conj (rest f) s (first f))
      `(assert ~(conj (rest f) s (first f))
               ~(str t ": " f " for " s)))))

(defn- assert-locals
  "Builds the individual type checks and validations for all the symbols tested by a particular expression or type."
  [f l req is-pred? input-map is-output? return]
  (let [opt-text (if is-output?
                   "Function return value failed condition"
                   "Optional arg failed condition")
        req-text (if is-output?
                   "Function return value failed condition"
                   "Arg condition failed")]
    (for [i l]
      (if (or (req i) (= input-map i) (= return i))
        (assert-sym f i req-text is-pred?)
        (if is-pred?
          `(if ~i
             ~(assert-sym f i opt-text is-pred?)
             true)
          `(when ~i
             ~(assert-sym f i opt-text is-pred?)))))))

(defn- build-asserts
  "Maps over all the different type checks and validation expressions and builds the tests for each symbol in each check."
  [argsplits is-pred? input-map is-output? return]
  (mapcat (fn [[f l]]
            (assert-locals f l (:req argsplits) is-pred? input-map is-output? return))
          (dissoc argsplits :req :opt :no-fn :defaults :output-validations)))

(defn- all-validations
  "Asserts, if necessary, that all required parameters are provided by the caller, then builds the validation and type checks for each parameter."
  [req f arg-analysis is-pred? input-map return]
  (if is-pred?
    [`(empty? (filter nil? ~(vec req)))
     (build-asserts arg-analysis is-pred? input-map false return)]
    [`(assert (empty? (filter nil? ~(vec req)))
              (str "Required named arguments to "
                   ~(clojure.string/upper-case (str f))
                   " missing."))
     (build-asserts arg-analysis is-pred? input-map false return)]))

(defn- transform-output-validations
  "Prepares an output validation list for processing."
  [validmap return]
  (reduce #(assoc % %2 [return])
          {:req #{return} :opt #{}}
          (:output-validations validmap)))

;; Easy mechanism to disable all assertions and checks when building functions.
(defonce use-assertions (atom true))
(defn off [] (reset! use-assertions false))
(defn on [] (reset! use-assertions true))

(defmacro df-build
  "Universal function constructor used by the public macros."
  [is-pred? begin f m docstring args output return body]
  (let [pre# (when (is-pre-or-post (first body)) (first body))
        arg-analysis (arg-split args m false return)
        arg-outs (arg-split output m true return)
        arg-outs (update-in arg-outs [:output-validations]
                            concat (:output-validations arg-analysis))
        {:keys [opt req defaults]} arg-analysis]
    `(~begin ~@(if (= 'fn begin) [f] [f docstring])
             [{:keys ~(vec (concat opt req))
               :or ~(apply hash-map (flatten defaults)) :as ~m}]
             ~@(if (or is-pred? @use-assertions)
                 (apply list* (or pre# `(comment "No pre or post map provided"))
                        (if is-pred?
                          `(((and ~@(apply list*
                                           (all-validations
                                            req f arg-analysis is-pred? m return)))))
                          (all-validations req f arg-analysis is-pred? m return)))
                 `((comment "Assertions are turned off.")))
             ~@(if (not is-pred?) 
                 (let [b (if pre# (rest body) body)
                       outputted (transform-output-validations arg-outs return)]
                   `((let [~return (do ~@b)]
                       ;; (comment "in: " ~arg-analysis)
                       ;; (comment "out: " ~arg-outs)
                       ;; (comment "outputted:" ~outputted)
                       ~@(build-asserts outputted false m true return)
                       ~return)))
                 ()))))

(defn- throw-arity-exception []
  (throw-text "Invalid parameters. Available arities:
[fn-name [input-map-name 
          doc-string 
          output-validation-list] 
arg-vector & body] 
Optional arguments shown in brackets may be in any order. "))

(defn- process-arg-loop
  "Helper for process-args. Analyzes the forms provided to the function definition."
  [args is-pred? begin]
  (loop [process args
         finished false
         analysis {}]
    (if finished analysis
        (cond
          (string? (first process))
          (cond (= 'fn begin) (throw-text "Anonymous fns can't have doc strings.")
                (:doc analysis) (throw-text "More than one doc string provided.")
                :else (recur (rest process) false
                             (assoc analysis :doc (first process))))
          
          (symbol? (first process))
          (if (:input analysis)
            (throw-text "More than one symbol provided to name input map.")
            (recur (rest process) false
                   (assoc analysis :input (first process))))

          (list? (first process))
          (if is-pred?
            (throw-text "Predicate functions cannot validate output.")
            (if (:output analysis)
              (throw-text "More than one output validation list provided.")
              (recur (rest process) false
                     (assoc analysis :output (first process)))))

          ;; experimental, undocumented feature
          (map? (first process))
          (cond
            is-pred? (throw-text "Predicate functions cannot name or validate output.")
            (not= 1 (count (first process))) (throw-text "One key and value allowed for custom in/out name map.")
            (:input analysis) (throw-text "More than one symbol provided to name input map.")
            :else (recur (rest process) false
                         (assoc analysis :input (first (first (first process)))
                                :return (second (first (first process))))))
                    
          (vector? (first process))
          (if (:v-args analysis)
            (throw-text "More than one arg vector provided.")
            (recur (rest process) true
                   (assoc analysis :v-args (first process)
                          :body (rest process))))
          
          :else (throw-arity-exception)))))

(defmacro process-args
  "Analyzes the forms provided to the function definition."
  [is-pred? begin f args]
  (if (and is-pred? (= :predfn f))
    (assert (vector? (first args)) "predfn accepts a vector only.")
    (assert (symbol? f) "First argument must be a symbol to name your function."))
  (let [{:keys [input doc v-args body output return]
         :or {doc "No doc string provided."
              return (symbol (str f "-return"))
              input (symbol (str f "-input"))}}
        (process-arg-loop args is-pred? begin)]
    (if (and is-pred? (seq body))
      (throw-text "Predicates do not have bodies or a :pre/:post map. Use the arg list to specify all conditions.")
      (let [ff (if (and is-pred? (= :predfn f)) (gensym) f)]
        (list `df-build is-pred? begin ff input doc v-args output return body)))))

;; The public macros for building defns that provide checks:

(defmacro pred
  [f & args]
  (list `process-args true 'defn f args))

(defmacro predfn
  [& args]
  (list `process-args true 'fn :predfn args))

(defmacro pred-
  [f & args]
  (list `process-args true 'defn- f args))

(defmacro df
  [f & args]
  (list `process-args false 'defn f args))

(defmacro df-
  [f & args]
  (list `process-args false 'defn- f args))

(defmacro dfn
  [f & args]
  (list `process-args false 'fn f args))

(defn get-or
  "Gets the default arg list, if any, for a function-quoted symbol"
  [sym]
  (-> sym meta :arglists first first :or))

(defmacro getor
  "Quotes the symbol and then calls get-or to get the default arg list."
  [sym]
  `(get-or #'~sym))

(defn transform-or-map
  "Takes an :or map and makes it a real map"
  ([m] (transform-or-map m keyword))
  ([m f]
   (into {}
         (map (fn [[k v]]
                (when v
                  [(f k) v])) m))))

(defmacro desc-defaults
  "Takes a sequence of symbols as previously defined with describe, and builds a map of all merged defaults from all function arg lists in those symbols' definitions. Optional function will map over the keyword so you can keep the original symbol, or get a keyword, or make a string, etc."
  ([r] `(desc-defaults ~r keyword))
  ([r f]
   (let [dfs (map #(symbol (str "make-" %)) r)]
     `(merge ~@(map (fn [x] `(transform-or-map (getor ~x) ~f)) dfs)))))

(defmacro defaults-vec
  "Takes a series of symbols at run-time and builds a defaults vector based on the default values for all the descriptions those symbols represent."
  [descs]
  `(vec (flatten (seq (desc-defaults ~descs identity)))))

(defn- object-build
  [title arglist d p]
  (assert (symbol? title) "Name to describe must be symbol")
  (assert (vector? arglist) "Second arg to describe must be a vector for the arglist.")
  (let [m (symbol (str title "-input"))
        make-name (symbol (str "make-" title))]
    `(do (~d ~make-name
             ~m
             ~(str "Builds and returns a map meeting the " title " requirements")
             ~arglist
             (let [defaults# (getor ~make-name)]
               (merge (transform-or-map defaults#) ~m)))
         (~p ~(symbol (str title "?"))
             ~m
             ~(str "Verifies if the supplied map matches the " title " structure.")
             ~arglist))))

(defmacro describe
  [title arglist]
  (object-build title arglist 'df 'pred))

(defmacro describe-
  [title arglist]
  (object-build title arglist 'df- 'pred-))

(defmacro desc
  [title arglist]
  (object-build title arglist 'df 'pred))

(defmacro desc-
  [title arglist]
  (object-build title arglist 'df- 'pred-))

;; Helper functions and macros

(defn see [f]
  (macroexpand-1 (macroexpand-1 (macroexpand-1 f))))

(def me macroexpand-1)

(defmacro c
  "Simply removes the layer of curly brackets so named parameters may be called directly. Instead of (somefunc {:a 1}) you can call (c somefunc :a 1)"
  [f & {:keys [] :as a}]
  (assert a (str "No map arguments provided. If none are to be passed, use (" f " {})"))
  `(~f ~a))

(defn ep>
  "Accepts a value and then tests all predicates supplied after, all of which must pass for a true return."
  [m & args]
  ((apply every-pred args) m))

(defn epcoll>
  "Similar to ep> but tests each item in a collection instead of a single item. Accepts a collection and any number of predicates. Tests that the collection is indeed a collection, and then runs every-pred on it. If used as validation expression, the collection is omitted as per thread-first and real predicate functions are all that you pass."
  ([coll pred]
   (and (coll? coll) (every? pred coll)))
  ([coll pred & preds]
   (epcoll> coll (apply every-pred pred preds))))

(defmacro c>
  "Re-orders the arguments to the c macro for use in a validation expression. "
  [v f k & ks]
  `(c ~f ~k ~v ~@ks))

(defmacro pred>
  "Creates and runs an anonymous predfn fn on the first argument, using the supplied argument validation vector."
  [m argl]
  `((predfn ~argl) ~m))

;; These omitted the need for the # around fn expressions, but in so doing,
;; they limited what you could pass, and were also confusingly different
;; to how you pass functions to ep> and epcoll>

;; (defn fn*>
;;   "Helper for and> and or>"
;;   [op v r] `((fn [x#] ((fn [~'%] (~op ~@r)) x#)) ~v))

;; (defmacro and>
;;   "Accepts a value and a series of real functions that operate on a value. All must pass for the and> to be true."
;;   [v & r] (fn*> 'and v r))

;; (defmacro or>
;;   "Like and> but uses or, not and. Only one of the supplied functions must pass."
;;   [v & r] (fn*> 'or v r))

(defn and>
  "Runs the supplied fns on the value, and requires all to pass."
  [v & r]
  (when (some (complement fn?) r)
    (throw-text "and> only accepts the test value followed by functions"))
  (if (seq r)
    (if ((first r) v)
      (apply and> v (rest r))
      false)
    true))

(defn or>
  "Runs the supplied fns on the value, and requires at least one to pass."
  [v & r]
  (when (some (complement fn?) r)
    (throw-text "or> only accepts the test value followed by functions"))
  (if (seq r)
    (if ((first r) v)
      true
      (apply or> v (rest r)))
    false))

(defn t
  "Gets the actual function associated with a keyword type check, as in the type-checks map up top."
  [k]
  (if-let [f (get type-checks k)]
    (second f)
    (throw-text (str "Invalid type keyword provided: " k))))

(defmacro g
  "A simple macro to facilitate syntax for pulling out data in nested structured. (g a.b.c) is the same as (get-in a [:b :c]) but provides a bit more of an OOP feel to it. Works on keywords only."
  [s]
  (let [a (clojure.string/split (str s) #"[.]")
        l (symbol (first a))
        r (rest a)]
    `(get-in ~l ~(vec (map keyword r)))))


