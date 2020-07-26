;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.generator
  (:require [clojure.test.check.generators :as gen]
            [neanderthal-stick.test-util :refer [MAX-SIZE]]
            [neanderthal-stick.internal.common :refer [entry-type-kw]]))

(def gen-size (gen/frequency [[5 (gen/return 0)]
                              [50 (gen/large-integer* {:min 1 :max 10})]
                              [35 (gen/large-integer* {:min 11 :max 100})]
                              [10 (gen/large-integer* {:min 101 :max MAX-SIZE})]]))

(def gen-band (gen/bind (gen/tuple gen-size gen-size)
                        (fn [[^long m ^long n]]
                          (gen/tuple (gen/return m)
                                     (gen/return n)
                                     (gen/large-integer* {:min 0 :max (max 0 (dec m))})
                                     (gen/large-integer* {:min 0 :max (max 0 (dec n))})))))

(def gen-sym-band (gen/bind gen-size
                            (fn [^long n]
                              (gen/tuple (gen/return n)
                                         (gen/large-integer* {:min 0 :max (max 0 (dec n))})))))

(def gen-subvec-indexes (gen/bind gen-sym-band
                                  (fn [[^long n ^long l]]
                                    (gen/tuple (gen/return n)
                                               (gen/large-integer* {:min 0 :max (max 0 (dec (- n l)))})
                                               (gen/return l)))))

(def gen-submatrix-indexes (gen/bind gen-band
                                     (fn [[^long m ^long n ^long k ^long l]]
                                       (gen/tuple (gen/return m)
                                                  (gen/return n)
                                                  (gen/large-integer* {:min 0 :max (max 0 (dec (- m k)))})
                                                  (gen/large-integer* {:min 0 :max (max 0 (dec (- n l)))})
                                                  (gen/return k)
                                                  (gen/return l)))))

(def gen-sym-submatrix-indexes (gen/bind gen-sym-band
                                         (fn [[^long n ^long k]]
                                           (gen/tuple (gen/return n)
                                                      (gen/large-integer* {:min 0 :max (max 0 (dec (- n k)))})
                                                      (gen/return k)))))

(def gen-banded-submatrix-indexes (gen/bind gen-band
                                            (fn [[^long m ^long n ^long kl ^long ku]]
                                              (gen/tuple (gen/return m)
                                                         (gen/return n)
                                                         (gen/return kl)
                                                         (gen/return ku)
                                                         (gen/large-integer* {:min 0 :max (max 0 (dec m))})
                                                         (gen/large-integer* {:min 0 :max (max 0 (dec n))})))))

(def gen-layout (gen/elements [:column :row]))
(def gen-uplo (gen/elements [:upper :lower]))
(def gen-diag (gen/elements [:unit :non-unit]))
(def gen-full-options (gen/hash-map :uplo gen-uplo
                                    :diag gen-diag
                                    :layout gen-layout))

(def gen-col-low-or-row-upper-options (gen/let [uplo gen-uplo]
                                        (gen/hash-map :uplo (gen/return uplo)
                                                      :layout (gen/return (if (= uplo :lower) :column :row)))))

(defn gen-ge-descriptor [factory]
  (gen/hash-map :kind (gen/return :matrix)
                :entry-type (gen/return (entry-type-kw factory))
                :matrix-type (gen/return :ge)
                :m gen-size
                :n gen-size
                :options (gen/hash-map :layout gen-layout)))

(defn gen-sy-descriptor [factory]
  (gen/hash-map :kind (gen/return :matrix)
                :entry-type (gen/return (entry-type-kw factory))
                :matrix-type (gen/return :sy)
                :n gen-size
                :options gen-col-low-or-row-upper-options))

(defn gen-tr-descriptor [factory]
  (gen/hash-map :kind (gen/return :matrix)
                :entry-type (gen/return (entry-type-kw factory))
                :matrix-type (gen/return :tr)
                :n gen-size
                :options gen-full-options))

(defn gen-gb-descriptor [factory]
  (gen/let [[m n kl ku] gen-band]
    (gen/hash-map :kind (gen/return :matrix)
                  :entry-type (gen/return (entry-type-kw factory))
                  :matrix-type (gen/return :gb)
                  :m (gen/return m)
                  :n (gen/return n)
                  :kl (gen/return kl)
                  :ku (gen/return ku)
                  :options (gen/hash-map :layout gen-layout))))

(defn gen-sb-descriptor [factory]
  (gen/let [[n k] gen-sym-band]
    (gen/hash-map :kind (gen/return :matrix)
                  :entry-type (gen/return (entry-type-kw factory))
                  :matrix-type (gen/return :sb)
                  :n (gen/return n)
                  :k (gen/return k)
                  :options gen-col-low-or-row-upper-options)))

(defn gen-tb-descriptor [factory]
  (gen/let [[n k] gen-sym-band]
    (gen/hash-map :kind (gen/return :matrix)
                  :entry-type (gen/return (entry-type-kw factory))
                  :matrix-type (gen/return :tb)
                  :n (gen/return n)
                  :k (gen/return k)
                  :options (gen/let [{:keys [uplo layout]} gen-col-low-or-row-upper-options]
                             (gen/hash-map :uplo (gen/return uplo)
                                           :layout (gen/return layout)
                                           :diag gen-diag)))))

(defn gen-sp-descriptor [factory]
  (gen/hash-map :kind (gen/return :matrix)
                :entry-type (gen/return (entry-type-kw factory))
                :matrix-type (gen/return :sp)
                :n gen-size
                :options (gen/hash-map :uplo gen-uplo
                                       :layout gen-layout)))

(defn gen-tp-descriptor [factory]
  (gen/hash-map :kind (gen/return :matrix)
                :entry-type (gen/return (entry-type-kw factory))
                :matrix-type (gen/return :tp)
                :n gen-size
                :options gen-full-options))

(defn gen-diag-descriptor [factory matrix-type]
  (gen/hash-map :kind (gen/return :matrix)
                :entry-type (gen/return (entry-type-kw factory))
                :matrix-type (gen/return matrix-type)
                :n gen-size))

(defn gen-gd-descriptor [factory] (gen-diag-descriptor factory :gd))
(defn gen-gt-descriptor [factory] (gen-diag-descriptor factory :gt))
(defn gen-dt-descriptor [factory] (gen-diag-descriptor factory :dt))
(defn gen-st-descriptor [factory] (gen-diag-descriptor factory :st))

(defn gen-vector-descriptor [factory]
  (gen/hash-map :kind (gen/return :vector)
                :entry-type (gen/return (entry-type-kw factory))
                :n gen-size))

(defn gen-matrix-descriptor [factory]
  (gen/one-of ((juxt gen-ge-descriptor gen-tr-descriptor gen-gb-descriptor
                     gen-sy-descriptor gen-sb-descriptor gen-tb-descriptor
                     gen-sp-descriptor gen-tp-descriptor gen-gt-descriptor
                     gen-gd-descriptor gen-dt-descriptor gen-st-descriptor) factory)))