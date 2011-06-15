(ns clj-schulze.test.core
  (:use [clj-schulze.core] :reload)
  (:use [clojure.test]))

(deftest ballot-validation
         (let [candidates #{:a :b :c :d}]
           (is (false? (valid-ballot? '(:a :b :c :d) candidates)))
           (is (false? (valid-ballot? [:a :b :c :d :c] candidates)))
           (is (false? (valid-ballot? [:a #{:b :c} :d :c] candidates)))
           (is (false? (valid-ballot? [:a :b :c #{:d :c}] candidates)))
           (is (false? (valid-ballot? [:a :b :e :c :d] candidates)))
           (is (false? (valid-ballot? [:a :b #{:c #{:d}}] candidates)))
           (is (false? (valid-ballot? [:a :b :c #{#{}} :d] candidates)))
           (is (false? (valid-ballot? [:a :b #{:c :d :e}] candidates)))
           (is (true? (valid-ballot? [:a :b :c :d] candidates)))
           (is (true? (valid-ballot? [:a :b #{:c :d}] candidates)))
           (is (true? (valid-ballot? [#{:a :b :c :d}] candidates)))
           (is (true? (valid-ballot? [:a #{:b :c} #{} :d] candidates)))))

(deftest candidate-validation
         (is (false? (valid-candidates? '(:a :b :c :d))))
         (is (false? (valid-candidates? #{:a :b :c :d "e"})))
         (is (false? (valid-candidates? #{:a :b :c :d #{:e}})))
         (is (false? (valid-candidates? #{:a :b :c :d #{} :f})))
         (is (true? (valid-candidates? #{:a})))
         (is (true? (valid-candidates? #{:a :b :c :d}))))

(deftest canonical-ballots
         (is (= (canonical-ballot [:a :b :c :d :e])
                [#{:a} #{:b} #{:c} #{:d} #{:e}]))
         (is (= (canonical-ballot [:a #{:b :c} :d :e])
                [#{:a} #{:c :b} #{:d} #{:e}]))
         (is (= (canonical-ballot [:a #{:b :c} #{:d} :e])
                [#{:a} #{:c :b} #{:d} #{:e}]))
         (is (= (canonical-ballot [:a #{:b :c} #{} :d :e])
                [#{:a} #{:c :b} #{:d} #{:e}]))
         (is (= (canonical-ballot [#{:a :b :c :d :e}])
                [#{:a :b :c :d :e}])))

(deftest valid-and-canonical
         (is (= (validate-and-canonicalize
                  [[:a :b :c :d],
                   [:a #{:b :c} :d],
                   [#{:b :e} :c],
                   [:b :a :e]]
                  #{:a :b :c :d :e})
                {[#{:a} #{:b} #{:c} #{:d} #{:e}] 1,
                 [#{:a} #{:c :b} #{:d} #{:e}] 1,
                 [#{:b :e} #{:c} #{:a :d}] 1,
                 [#{:b} #{:a} #{:e} #{:c :d}] 1})))

; From the Wikipedia article "Schulze method", available at
; http://en.wikipedia.org/w/index.php?title=Schulze_method&oldid=432829763
(deftest wikipedia-example
         (let [candidates #{:a :b :c :d :e},
               ballots (concat (repeat 5 [:a :c :b :e :d])
                                (repeat 5 [:a :d :e :c :b])
                                (repeat 8 [:b :e :d :a :c])
                                (repeat 3 [:c :a :b :e :d])
                                (repeat 7 [:c :a :e :b :d])
                                (repeat 2 [:c :b :a :d :e])
                                (repeat 7 [:d :c :e :b :a])
                                (repeat 8 [:e :b :a :d :c])),
               defeats (total-pairwise-defeats
                         (validate-and-canonicalize ballots candidates)
                         candidates),
               paths (strongest-paths defeats candidates),
               winner (schulze-winner paths candidates)]
           (is (= defeats
                  {[:a :b] 20, [:a :c] 26, [:a :d] 30, [:a :e] 22,
                   [:b :a] 25, [:b :c] 16, [:b :d] 33, [:b :e] 18,
                   [:c :a] 19, [:c :b] 29, [:c :d] 17, [:c :e] 24,
                   [:d :a] 15, [:d :b] 12, [:d :c] 28, [:d :e] 14,
                   [:e :a] 23, [:e :b] 27, [:e :c] 21, [:e :d] 31}))
           (is (= paths
                  {[:a :b] 28, [:a :c] 28, [:a :d] 30, [:a :e] 24,
                   [:b :a] 25, [:b :c] 28, [:b :d] 33, [:b :e] 24,
                   [:c :a] 25, [:c :b] 29, [:c :d] 29, [:c :e] 24,
                   [:d :a] 25, [:d :b] 28, [:d :c] 28, [:d :e] 24,
                   [:e :a] 25, [:e :b] 28, [:e :c] 28, [:e :d] 31}))
           (is (= winner :e))))

; From "A New Monotonic, Clone-Independent, Reversal-Symmetric, and Condorcet-
; Consistent Single-Winner Election Method" by Markus Schulze, available from
; http://m-schulze.webhop.net/
(deftest schulze-example1
         (let [candidates #{:a :b :c :d},
               ballots (concat (repeat 8 [:a :c :d :b])
                               (repeat 2 [:b :a :d :c])
                               (repeat 4 [:c :d :b :a])
                               (repeat 4 [:d :b :a :c])
                               (repeat 3 [:d :c :b :a])),
               defeats (total-pairwise-defeats
                         (validate-and-canonicalize ballots candidates)
                         candidates),
               paths (strongest-paths defeats candidates),
               winner (schulze-winner paths candidates)]
           (is (= defeats
                  {[:a :b] 8, [:a :c] 14, [:a :d] 10,
                   [:b :a] 13, [:b :c] 6, [:b :d] 2,
                   [:c :a] 7, [:c :b] 15, [:c :d] 12,
                   [:d :a] 11, [:d :b] 19, [:d :c] 9}))
           (is (= winner :d))))

; From "A New Monotonic, Clone-Independent, Reversal-Symmetric, and Condorcet-
; Consistent Single-Winner Election Method" by Markus Schulze, available from
; http://m-schulze.webhop.net/
(deftest schulze-example2
         (let [candidates #{:a :b :c :d},
               ballots (concat (repeat 3 [:a :b :c :d])
                               (repeat 2 [:c :b :d :a])
                               (repeat 2 [:d :a :b :c])
                               (repeat 2 [:d :b :c :a])),
               defeats (total-pairwise-defeats
                         (validate-and-canonicalize ballots candidates)
                         candidates),
               paths (strongest-paths defeats candidates),
               winner (schulze-winner paths candidates)]
           (is (= defeats
                  {[:a :b] 5, [:a :c] 5, [:a :d] 3,
                   [:b :a] 4, [:b :c] 7, [:b :d] 5,
                   [:c :a] 4, [:c :b] 2, [:c :d] 5,
                   [:d :a] 6, [:d :b] 4, [:d :c] 4}))
           (is (= winner #{:b :d}))))

; From "A New Monotonic, Clone-Independent, Reversal-Symmetric, and Condorcet-
; Consistent Single-Winner Election Method" by Markus Schulze, available from
; http://m-schulze.webhop.net/
(deftest schulze-example3
         (let [candidates #{:a :b :c :d},
               ballots (concat (repeat 6 [:a :b :c :d])
                               (repeat 12 [:a :c :d :b])
                               (repeat 21 [:b :c :a :d])
                               (repeat 9 [:c :d :b :a])
                               (repeat 15 [:d :b :a :c])),
               defeats (total-pairwise-defeats
                         (validate-and-canonicalize ballots candidates)
                         candidates),
               paths (strongest-paths defeats candidates),
               winner (schulze-winner paths candidates)]
           (is (= defeats
                  {[:a :b] 18, [:a :c] 33, [:a :d] 39,
                   [:b :a] 45, [:b :c] 42, [:b :d] 27,
                   [:c :a] 30, [:c :b] 21, [:c :d] 48,
                   [:d :a] 24, [:d :b] 36, [:d :c] 15}))
           (is (= winner :b))))

; From "A New Monotonic, Clone-Independent, Reversal-Symmetric, and Condorcet-
; Consistent Single-Winner Election Method" by Markus Schulze, available from
; http://m-schulze.webhop.net/
(deftest schulze-example4-situation1
         (let [candidates #{:a :b :c :d :e :f},
               ballots (concat (repeat 3 [:a :d :e :b :c :f])
                               (repeat 3 [:b :f :e :c :d :a])
                               (repeat 4 [:c :a :b :f :d :e])
                               (repeat 1 [:d :b :c :e :f :a])
                               (repeat 4 [:d :e :f :a :b :c])
                               (repeat 2 [:e :c :b :d :f :a])
                               (repeat 2 [:f :a :c :d :b :e])),
               defeats (total-pairwise-defeats
                         (validate-and-canonicalize ballots candidates)
                         candidates),
               paths (strongest-paths defeats candidates),
               winner (schulze-winner paths candidates)]
           (is (= defeats
                  {[:a :b] 13, [:a :c] 9, [:a :d] 9, [:a :e] 9, [:a :f] 7,
                   [:b :a] 6, [:b :c] 11, [:b :d] 9, [:b :e] 10, [:b :f] 13,
                   [:c :a] 10, [:c :b] 8, [:c :d] 11, [:c :e] 7, [:c :f] 10,
                   [:d :a] 10, [:d :b] 10, [:d :c] 8, [:d :e] 14, [:d :f] 10,
                   [:e :a] 10, [:e :b] 9, [:e :c] 12, [:e :d] 5, [:e :f] 10,
                   [:f :a] 12, [:f :b] 6, [:f :c] 9, [:f :d] 9, [:f :e] 9}))
           (is (= winner :a))))

; From "A New Monotonic, Clone-Independent, Reversal-Symmetric, and Condorcet-
; Consistent Single-Winner Election Method" by Markus Schulze, available from
; http://m-schulze.webhop.net/
(deftest schulze-example4-situation2
         (let [candidates #{:a :b :c :d :e :f},
               ballots (concat (repeat 3 [:a :d :e :b :c :f])
                               (repeat 3 [:b :f :e :c :d :a])
                               (repeat 4 [:c :a :b :f :d :e])
                               (repeat 1 [:d :b :c :e :f :a])
                               (repeat 4 [:d :e :f :a :b :c])
                               (repeat 2 [:e :c :b :d :f :a])
                               (repeat 2 [:f :a :c :d :b :e])
                               (repeat 2 [:a :e :f :c :b :d])),
               defeats (total-pairwise-defeats
                         (validate-and-canonicalize ballots candidates)
                         candidates),
               paths (strongest-paths defeats candidates),
               winner (schulze-winner paths candidates)]
           (is (= defeats
                  {[:a :b] 15, [:a :c] 11, [:a :d] 11, [:a :e] 11, [:a :f] 9,
                   [:b :a] 6, [:b :c] 11, [:b :d] 11, [:b :e] 10, [:b :f] 13,
                   [:c :a] 10, [:c :b] 10, [:c :d] 13, [:c :e] 7, [:c :f] 10,
                   [:d :a] 10, [:d :b] 10, [:d :c] 8, [:d :e] 14, [:d :f] 10,
                   [:e :a] 10, [:e :b] 11, [:e :c] 14, [:e :d] 7 , [:e :f] 12,
                   [:f :a] 12, [:f :b] 8, [:f :c] 11, [:f :d] 11, [:f :e] 9}))
           (is (= winner :d))))

; From "A New Monotonic, Clone-Independent, Reversal-Symmetric, and Condorcet-
; Consistent Single-Winner Election Method" by Markus Schulze, available from
; http://m-schulze.webhop.net/
(deftest schulze-example5-situation1
         (let [candidates #{:a :b :c :d},
               ballots (concat (repeat 3 [:a :b :d :c])
                               (repeat 5 [:a :d :b :c])
                               (repeat 1 [:a :d :c :b])
                               (repeat 2 [:b :a :d :c])
                               (repeat 2 [:b :d :c :a])
                               (repeat 4 [:c :a :b :d])
                               (repeat 6 [:c :b :a :d])
                               (repeat 2 [:d :b :c :a])
                               (repeat 5 [:d :c :a :b])),
               defeats (total-pairwise-defeats
                         (validate-and-canonicalize ballots candidates)
                         candidates),
               paths (strongest-paths defeats candidates),
               winner (schulze-winner paths candidates)]
           (is (= defeats
                  {[:a :b] 18, [:a :c] 11, [:a :d] 21,
                   [:b :a] 12, [:b :c] 14, [:b :d] 17,
                   [:c :a] 19, [:c :b] 16, [:c :d] 10,
                   [:d :a] 9, [:d :b] 13, [:d :c] 20}))
           (is (= winner :a))))

; From "A New Monotonic, Clone-Independent, Reversal-Symmetric, and Condorcet-
; Consistent Single-Winner Election Method" by Markus Schulze, available from
; http://m-schulze.webhop.net/
(deftest schulze-example5-situation2
         (let [candidates #{:a :b :c :d :e},
               ballots (concat (repeat 3 [:a :b :d :e :c])
                               (repeat 5 [:a :d :e :b :c])
                               (repeat 1 [:a :d :e :c :b])
                               (repeat 2 [:b :a :d :e :c])
                               (repeat 2 [:b :d :e :c :a])
                               (repeat 4 [:c :a :b :d :e])
                               (repeat 6 [:c :b :a :d :e])
                               (repeat 2 [:d :b :e :c :a])
                               (repeat 5 [:d :e :c :a :b])),
               defeats (total-pairwise-defeats
                         (validate-and-canonicalize ballots candidates)
                         candidates),
               paths (strongest-paths defeats candidates),
               winner (schulze-winner paths candidates)]
           (is (= defeats
                  {[:a :b] 18, [:a :c] 11, [:a :d] 21, [:a :e] 21,
                   [:b :a] 12, [:b :c] 14, [:b :d] 17, [:b :e] 19,
                   [:c :a] 19, [:c :b] 16, [:c :d] 10, [:c :e] 10,
                   [:d :a] 9, [:d :b] 13, [:d :c] 20, [:d :e] 30,
                   [:e :a] 9, [:e :b] 11, [:e :c] 20, [:e :d] 0}))
           (is (= winner :b))))
