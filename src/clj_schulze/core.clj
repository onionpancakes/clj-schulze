; clj-schulze
; A Clojure implementation of the Schulze voting method.
;
; https://github.com/bdesham/clj-schulze
;
; Copyright (c) 2011, Benjamin D. Esham.  This program is released under the MIT
; license, which can be found in the "License" section of the file README.md.

(ns clj-schulze.core
  (:use [clojure.set :only (difference)]))

;; # Utility functions

(defn flatten-sets
  "Like flatten, but pulls elements out of sets instead of sequences."
  [v]
  (filter (complement set?)
          (rest (tree-seq set? seq (set v)))))

; with thanks to gfrlog in #clojure
(defn assoc-value-with-each
  "Takes a sequence of two-element vectors where each first item is a list and
  each second item is some value. Returns a sequence of maps where the values
  are now associated with *each item* in the old first-element lists, instead of
  being associated with the lists themselves.

  Example: `(assoc-value-with-each [['(:a :b :c) 3], [[:d :e] 5]])`
  -> `({:a 3}, {:b 3}, {:c 3}, {:d 5}, {:e 5})`"
  [pairs]
  (mapcat (fn [[lst cnt]] (for [k lst] {k cnt}))
          pairs))

(defn change-first-elements
  "Apply the function f to the first element of each of the given two-element
  vectors."
  [f vv]
  (map (fn [[a b]] [(f a) b]) vv))

;; # Validation, canonicalization, etc.

(defn valid-ballot?
  "Make sure that the ballot is a vector; contains no duplicate entries,
  including in nested sets; and contains only entries which appear in the set of
  candidates."
  [ballot candidates]
  (and (vector? ballot)
       (when-let [fb (flatten-sets ballot)]
         (and (apply distinct? fb)
              (every? #(candidates %) fb)))))

(defn valid-ballots?
  "Check every ballot in a sequence with validate-ballot."
  [ballots candidates]
  (every? #(valid-ballot? % candidates) ballots))

(defn valid-candidates?
  "Make sure that the candidates set *is* a set and consists entirely of
  keywords."
  [candidates]
  (and (set? candidates)
       (every? keyword? candidates)))

(defn add-missing-candidates
  "If any valid candidates don't appear on the ballot, add them in a set at the
  end of the ballot. (This adds an empty set if all of the valid candidates have
  already been listed, but that's taken care of by canonical-ballot.)"
  [ballot candidates]
  (conj ballot (difference candidates (flatten-sets ballot))))

(defn canonical-element
  "Return a ballot element, converted to canonical form. This means that
  keywords are wrapped in sets and sets are flattened."
  [element]
  (if (keyword? element)
    (set (vector element))
    (set (flatten-sets element))))

(defn valid-element?
  "Make sure that a ballot element is either a keyword or a set. In the latter
  case, make sure that the set isn't something daft like #{#{}} or #{#{#{}}}."
  [element]
  (or (keyword element)
      (and (seq element)
           (seq (flatten-sets element)))))

(defn canonical-ballot
  "Return the ballot with the following changes: all keywords not in a set are
  converted to one-element sets; any nested sets (?!) are flattened; empty sets
  are removed."
  [ballot]
  (map canonical-element (filter valid-element? ballot)))

(defn validate-and-canonicalize
  "Takes a vector of ballots and a set of candidates. Validates the candidates
  set. Validates each ballot, adds any missing candidates, and converts it to
  canonical form. Ballots are passed through `frequencies` on their way out. If
  the candidates or the ballots are not valid, throws an exception."
  [ballots candidates]
  (when-not (valid-candidates? candidates)
    (throw (Exception. "Candidates set is not valid")))
  (when-not (valid-ballots? ballots candidates)
    (throw (Exception. "Ballots vector is not valid")))
  (frequencies
    (map (comp canonical-ballot #(add-missing-candidates % candidates))
         ballots)))

;; # Voting stuff

(defn pairwise-defeats
  "Takes a ballot and returns a set of two-element vectors. Each vector
  `[:a :b]` indicates that candidate a is preferred to candidate b. It is
  intended that outside callers will use the single-argument form."
  ([b] (pairwise-defeats b #{}))
  ([b s]
   (if (empty? (rest b))
     s
     (recur (rest b)
            (apply conj s (for [winner (seq (first b)),
                                loser (flatten-sets (rest b))]
                            [winner loser]))))))

(defn total-pairwise-defeats
  "Takes a (counted) hash of ballots and returns a counted hash of the pairwise
  defeats.
  
  The steps this function uses are, from inside to outside:

  1. Turn the input--a hash from ballots to occurrences--into a bunch of vectors
  where the first element is the ballot and the second is the number of
  occurrences

  2. Look at each first element (i.e. ballot) and call pairwise-defeats on it

  3. Split the vectors so that instead of a bunch of pairwise defeats each being
  associated with a number of occurrences, we associate each pairwise defeat
  individually with a number of occurrences

  4. Combine all of the resulting hashes so that we have the total number of
  occurrences for each pairwise defeat"
  [ballots]
  (apply merge-with +
         (assoc-value-with-each
           (change-first-elements pairwise-defeats (seq ballots)))))

(defn add-missing-pairs
  "Ensures that every possible pair of candidates is represented in the hash of
  pairwise defeats."
  [defeats candidates]
  (merge-with +
              (apply conj {} (for [a candidates,
                                   b candidates :when (not= a b)]
                               {[a b] 0}))
              defeats))

(defn gt-d
  [[x1 x2] [y1 y2]]
  (or (or (and (> x1 y1)
               (<= x2 y2))
          (and (>= x1 y1)
               (< x2 y2)))
      (or (and (> x1 x2)
               (<= y1 y2))
          (and (>= x1 x2)
               (< y1 y2)))))

(defn min-d
  [a b]
  (if (gt-d a b)
    b
    a))

(defn strongest-paths2
  "Calculates the strength of the strongest path between each pair of
  candidates."
  [defeats candidates]
  (let [p (apply conj (for [i candidates,
                            j candidates :when (not= i j)]
                          {[i j] [(defeats [i j]), (defeats [j i])]}))]
    (def p2 (ref p))
    (for [i candidates,
          j candidates :when (not= i j),
          k candidates :when (and (not= i k) (not= j k))]
      (if (gt-d (min-d (@p2 [j i]) (@p2 [i k])) (@p2 [j k]))
        (dosync (alter p2 assoc [j k] (min-d (@p2 [j i]) (@p2 [i k]))))))
    @p2))

;(defn strongest-paths
;  "Calculates the strength of the strongest path between each pair of
;  candidates."
;  [defeats candidates]
;  (let [p (apply conj (for [i candidates,
;                            j candidates :when (not= i j)]
;                        (if (> (defeats [i j]) (defeats [j i]))
;                          {[i j] (defeats [i j])}
;                          {[i j] 0})))]
;    (def p2 (ref p))
;    (for [i candidates,
;          j candidates :when (not= i j),
;          k candidates :when (and (not= i k) (not= j k))]
;      (dosync (alter p2 assoc [j k] (max (@p2 [j k]) (min (@p2 [j i]) (@p2 [i k]))))))
;    @p2))

;(defn strongest-paths
;  "Calculates the strength of the strongest path between each pair of
;  candidates."
;  [defeats candidates]
;  (let [p (into {} (doall (for [i candidates,
;                                j candidates :when (not= i j)]
;                            (if (> (defeats [i j]) (defeats [j i]))
;                              {[i j] (defeats [i j])}
;                              {[i j] 0}))))]
;    (loop [map {}]
;      (for [i candidates,
;            j candidates :when (not= i j),
;            k candidates :when (and (not= i k) (not= j k))]
;        (recur (assoc map [j k] (max (p [j k]) (min (p [j i]) (p [i k])))))))))



; vim: tw=80
; intended to be viewed with a window width of 108 columns
