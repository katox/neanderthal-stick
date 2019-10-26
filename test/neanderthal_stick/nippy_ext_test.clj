;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.nippy-ext-test
  (:require [midje.sweet :refer [fact facts throws => roughly truthy]]
            [midje.experimental :refer [for-all]]
            [clojure.test.check.generators :as gen]
            [uncomplicate.commons.core :refer [release with-release info]]
            [uncomplicate.neanderthal.core :refer [transfer! copy! dim subvector vctr submatrix ge tr sy gb sb tb tp sp gd gt dt st]]
            [uncomplicate.neanderthal.block :as block]
            [taoensso.nippy :as nippy]
            [neanderthal-stick.generator :refer :all]
            [neanderthal-stick.test-util :refer :all]
            [neanderthal-stick.nippy-ext])
  (:import (clojure.lang ExceptionInfo)))

(defn vector-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory MAX-SIZE)]
    (for-all
      [^long n gen-size]
      {:num-tests 100}
      (fact "Vector freeze/thaw should round-trip"
            (with-release [a (vctr factory (subvector vctr-source 0 n))
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn subvector-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory MAX-SIZE)]
    (for-all
      [[^long n ^long k ^long l] gen-subvec-indexes]
      {:num-tests 100}
      (fact "Vector freeze/thaw should round-trip"
            (with-release [x (vctr factory (subvector vctr-source 0 n))
                           a (subvector x k l)
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn ge-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (* MAX-SIZE MAX-SIZE))]
    (for-all
      [^long m gen-size
       ^long n gen-size
       layout gen-layout]
      {:num-tests 100}
      (fact "General matrix freeze/thaw should round-trip"
            (with-release [a (ge factory m n vctr-source {:layout layout})
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn submatrix-ge-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (* MAX-SIZE MAX-SIZE))]
    (for-all
      [[^long m ^long n ^long i ^long j ^long k ^long l] gen-submatrix-indexes
       layout gen-layout]
      {:num-tests 100}
      (fact "General submatrix freeze/thaw should round-trip"
            (with-release [x (ge factory m n vctr-source {:layout layout})
                           a (submatrix x i j k l)]
              (if (or (zero? (dim a)) (block/contiguous? a))
                (with-release [b (nippy/thaw (nippy/freeze a))]
                  (= a b) => true)
                (nippy/freeze a) => (throws ExceptionInfo)))))))

(defn tr-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (packed-items MAX-SIZE))]
    (for-all
      [^long n gen-size
       layout gen-layout
       uplo gen-uplo
       diag gen-diag]
      {:num-tests 100}
      (fact "Triangular matrix freeze/thaw should round-trip"
            (with-release [a (tr factory n vctr-source
                                 {:layout layout :uplo uplo :diag diag})
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn sy-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (packed-items MAX-SIZE))]
    (for-all
      [^long n gen-size
       layout gen-layout
       uplo gen-uplo]
      {:num-tests 100}
      (fact "Dense symmetric matrix freeze/thaw should round-trip"
            (with-release [a (sy factory n vctr-source
                                 {:layout layout :uplo uplo})
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn gb-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (banded-items MAX-SIZE (dec MAX-SIZE) (dec MAX-SIZE)))]
    (for-all
      [[^long m ^long n ^long kl ^long ku] gen-band
       layout gen-layout]
      {:num-tests 100}
      (fact "General banded matrix freeze/thaw should round-trip"
            (with-release [a (gb factory m n kl ku vctr-source
                                 {:layout layout})
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn sb-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (banded-items MAX-SIZE (dec MAX-SIZE) 0))]
    (for-all
      [[^long n ^long k] gen-sym-band
       layout gen-layout]
      {:num-tests 100}
      (fact "Symmetric banded matrix freeze/thaw should round-trip"
            (with-release [a (sb factory n k vctr-source
                                 {:layout layout
                                  :uplo   (if (= layout :column) :lower :upper)})
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn tb-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (banded-items MAX-SIZE (dec MAX-SIZE) 0))]
    (for-all
      [[^long n ^long k] gen-sym-band
       layout gen-layout
       diag gen-diag]
      {:num-tests 100}
      (fact "Triangular banded matrix freeze/thaw should round-trip"
            (with-release [a (tb factory n k vctr-source
                                 {:layout layout
                                  :uplo   (if (= layout :column) :lower :upper)
                                  :diag   diag})
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn tp-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (packed-items MAX-SIZE))]
    (for-all
      [^long n gen-size
       layout gen-layout
       uplo gen-uplo
       diag gen-diag]
      {:num-tests 100}
      (fact "Triangular packed matrix freeze/thaw should round-trip"
            (with-release [a (tp factory n vctr-source
                                 {:layout layout
                                  :uplo   uplo
                                  :diag   diag})
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn sp-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (packed-items MAX-SIZE))]
    (for-all
      [^long n gen-size
       layout gen-layout
       uplo gen-uplo]
      {:num-tests 100}
      (fact "Symmetric packed matrix freeze/thaw should round-trip"
            (with-release [a (sp factory n vctr-source
                                 {:layout layout
                                  :uplo   uplo})
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn gd-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory MAX-SIZE)]
    (for-all
      [^long n gen-size]
      {:num-tests 100}
      (fact "Diagonal matrix freeze/thaw should round-trip"
            (with-release [a (gd factory n vctr-source nil)
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn gt-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (tridiagonal-items MAX-SIZE))]
    (for-all
      [^long n gen-size]
      {:num-tests 100}
      (fact "Tridiagonal matrix freeze/thaw should round-trip"
            (with-release [a (gt factory n vctr-source nil)
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn dt-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (tridiagonal-items MAX-SIZE))]
    (for-all
      [^long n gen-size]
      {:num-tests 100}
      (fact "Diagonally dominant tridiagonal matrix freeze/thaw should round-trip"
            (with-release [a (dt factory n vctr-source nil)
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))

(defn st-round-trip-test [factory]
  (with-release [vctr-source (create-random-vector factory (bidiagonal-items MAX-SIZE))]
    (for-all
      [^long n gen-size]
      {:num-tests 100}
      (fact "Symmetric tridiagonal matrix freeze/thaw should round-trip"
            (with-release [a (st factory n vctr-source nil)
                           b (nippy/thaw (nippy/freeze a))]
              (= a b) => true)))))
