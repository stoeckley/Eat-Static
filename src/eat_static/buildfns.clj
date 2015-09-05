(ns eat-static.buildfns)

(defn is-bool?
  "Determines if the provided value is a true or false value."
  [x]
  (contains? #{true false} x))

;; clojure primitive type checks
;; quoted for use in macros
(def type-checks
  {:bool 'is-bool?
   :boolean 'is-bool?
   :b 'is-bool?
   :fn 'fn? 
   :sym 'symbol?
   :symbol 'symbol? 
   :k 'keyword?
   :key 'keyword?
   :keyword 'keyword?
   :str 'string?
   :string 'string?
   :c 'char?
   :char 'char?
   :anything 'identity
   :any 'identity
   :identity 'identity
   :m 'map?
   :map 'map?
   :v 'vector?
   :vec 'vector?
   :vector 'vector?
   :set 'set?
   :l 'list?
   :list 'list?
   :n 'number?
   :num 'number?
   :number 'number?
   :r 'ratio?
   :rat 'ratio?
   :ratio 'ratio?
   :f 'float?
   :float 'float?
   :i 'integer?
   :int 'integer?
   :integer 'integer?})

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
  [ass place sym input-map & {:keys [process remain]}]
  (let [l (:lastfn ass)
        put #(if (= sym input-map)
               (update-in % [%2] conj sym)
               (-> % (update-in [place] conj sym)
                   (update-in [%2] conj sym)))]
    (cond (= :finished process) ass
          process (-> ass (put process)
                      (place-symbol place sym input-map
                                    :process (or (first remain) :finished)
                                    :remain (next remain)))
          (vector? l) (place-symbol ass place sym input-map
                                    :process (first l)
                                    :remain (next l))
          :else (put ass l))))

(defn- arg-split-fn
  "This is the reduction function used when looking at all the forms provided in a function definition's argument vector, or an output validation list. It places symbols into their required contexts."
  [input-map is-output?]
  (fn [result arg]
    (cond
      (and (list? arg) (= '++ (first arg)))
      (do (assert (not is-output?) "A function's output validation list already acts like ++ and thus ++ should not be used.")
          (assert (every? (fn [x] (or (keyword? x) (list? x))) (rest arg)) ">> only accepts keywords or lists as validators.")
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
      (if is-output?
        (throw-text "Output validations always operate on the function's return value, and no other symbols may be named.")
        (let [sym (symbol-root arg)]
          (if (and (is-optional arg) (= input-map sym))
            (throw-text "Full input map cannot be optional.")
            (place-symbol result (if (is-optional arg) :opt :req) sym input-map))))

      (vector? arg)
      (if is-output?
        (throw-text
         "Output validations always operate on the function's return value, and no other symbols or default vectors may be named.")
        (let [vs (reverse arg)
              assigns (loop [v nil process vs r {}]
                        (if (seq process)
                          (if (not (symbol? (first process)))
                            (recur (first process) (rest process) r)
                            (if (= input-map (first process))
                              (throw-text "Full input map cannot be optional.")
                              (recur v (rest process)
                                     (assoc r (first process) v))))
                          r))]
          (reduce (fn [r [s default]]
                    (-> r (place-symbol :opt (symbol-root s) input-map)
                        (update-in [:defaults] conj [(symbol-root s) default])))
                  result assigns)))

      :else (if is-output?
              (throw-text "Output validation list list contained an element that is not a keyword or list.")
              (throw-text "Arg list contained an element that is not a symbol, vector, keyword, or list.")))))

(defn- arg-split
  "Splits and analyzes the vector of function arguments or the list of output validations."
  [args input-map is-output?]
  (dissoc
   (reduce (arg-split-fn input-map is-output?)
           {:lastfn :no-fn :opt #{} :req #{} :output-validations []}
           args)
   :lastfn))

(defn- assert-sym
  "Creates the assertion for a single test for a single symbol. Multiple calls will assert the same symbol for multiple different tests. Creates either an assertion statement or a simple function call, depending on whether the function is assertive or a true/false predicate function, non-assertive."
  [f s t is-pred?]
  (if (keyword? f)
    (if-let [kfn (get type-checks f)]
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
  [f l req is-pred? input-map is-output?]
  (let [opt-text (if is-output?
                   "Function return value failed condition"
                   "Optional arg failed condition")
        req-text (if is-output?
                   "Function return value failed condition"
                   "Arg condition failed")]
    (for [i l]
      (if (or (req i) (= input-map i))
        (assert-sym f i req-text is-pred?)
        (if is-pred?
          `(if ~i
             ~(assert-sym f i opt-text is-pred?)
             true)
          `(when ~i
             ~(assert-sym f i opt-text is-pred?)))))))

(defn- build-asserts
  "Maps over all the different type checks and validation expressions and builds the tests for each symbol in each check."
  [argsplits is-pred? input-map is-output?]
  (mapcat (fn [[f l]]
            (assert-locals f l (:req argsplits) is-pred? input-map is-output?))
          (dissoc argsplits :req :opt :no-fn :defaults)))

(defn- all-validations
  "Asserts, if necessary, that all required parameters are provided by the caller, then builds the validation and type checks for each parameter."
  [req f arg-analysis is-pred? input-map]
  (if is-pred?
    [`(empty? (filter nil? ~(vec req)))
     (build-asserts arg-analysis is-pred? input-map false)]
    [`(assert (empty? (filter nil? ~(vec req)))
              (str "Required named arguments to "
                   ~(clojure.string/upper-case (str f))
                   " missing."))
     (build-asserts arg-analysis is-pred? input-map false)]))

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
  [is-pred? begin f m docstring args output body]
  (let [pre# (when (is-pre-or-post (first body)) (first body))
        arg-analysis (arg-split args m false)
        arg-outs (arg-split output m true)
        {:keys [opt req defaults]} arg-analysis]
    `(~begin ~@(if (= 'fn begin) [f] [f docstring])
             [{:keys ~(vec (concat opt req))
               :or ~(apply hash-map (flatten defaults)) :as ~m}]
             ~@(if (or is-pred? @use-assertions)
                 (apply list* (or pre# `(comment "No pre or post map provided"))
                        (if is-pred?
                          `(((and ~@(apply list*
                                           (all-validations
                                            req f arg-analysis is-pred? m)))))
                          (all-validations req f arg-analysis is-pred? m)))
                 `((comment "Assertions are turned off.")))
             ~@(if (not is-pred?) 
                 (let [b (if pre# (rest body) body)
                       returnv (gensym (str f "-return-value"))
                       outputted (transform-output-validations arg-outs returnv)]
                   `((let [~returnv (do ~@b)]
                       ;; (comment "in: " ~arg-analysis)
                       ;; (comment "out: " ~arg-outs)
                       ;; (comment "outputted:" ~outputted)
                       ~@(build-asserts outputted false m true)
                       ~returnv)))
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
  (let [{:keys [input doc v-args body output]
         :or {doc "No doc string provided."
              input (symbol (str f "-input"))}}
        (process-arg-loop args is-pred? begin)]
    (if (and is-pred? (seq body))
      (throw-text "Predicates do not have bodies or a :pre/:post map. Use the arg list to specify all conditions.")
      (let [ff (if (and is-pred? (= :predfn f)) (gensym) f)]
        (list `df-build is-pred? begin ff input doc v-args output body)))))

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

(defn- object-build
  [title arglist d p]
  (let [m (gensym (str title))]
    `(do (~d ~(symbol (str "make-" title))
             ~m
             ~(str "Builds and returns a map meeting the " title " requirements")
             ~arglist ~m)
         (~p ~(symbol (str title "?"))
             ~(str "Verifies if the supplied map matches the " title " structure.")
             ~arglist))))

(defmacro object
  [title arglist]
  (object-build title arglist 'df 'pred))

(defmacro object-
  [title arglist]
  (object-build title arglist 'df- 'pred-))

;; Helper functions and macros

(defn see [f]
  (macroexpand-1 (macroexpand-1 (macroexpand-1 f))))

(def me macroexpand-1)

(defmacro c
  [f & {:keys [] :as a}]
  (assert a (str "No map arguments provided. If none are to be passed, use (" f " {})"))
  `(~f ~a))

(defn epcoll>
  ([coll pred]
   (and (coll? coll) (every? pred coll)))
  ([coll pred & preds]
   (epcoll> coll (apply every-pred pred preds))))

(defmacro c>
  [v f k & ks]
  `(c ~f ~k ~v ~@ks))

(defn ep>
  [m & args]
  ((apply every-pred args) m))

(defmacro pred>
  [m argl]
  `((predfn ~argl) ~m))

(defn fn*> [op v r] `((fn [x#] ((fn [~'%] (~op ~@r)) x#)) ~v))

(defmacro and> [v & r] (fn*> 'and v r))

(defmacro or> [v & r] (fn*> 'or v r))

(defmacro g
  "A simple macro to facilitate syntax for pulling out data in nested structured. (g a.b.c) is the same as (get-in a [:b :c]) but provides a bit more of an OOP feel to it. Works on keywords only."
  [s]
  (let [a (clojure.string/split (str s) #"[.]")
        l (symbol (first a))
        r (rest a)]
    `(get-in ~l ~(vec (map keyword r)))))


