;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.test-util
  (:require [uncomplicate.commons.core :refer [release with-release info]]
            [uncomplicate.neanderthal.core :refer [transfer! vctr]]
            [uncomplicate.neanderthal.block :refer [entry-type]]
            [uncomplicate.neanderthal.native :refer [native-int native-long native-double]]
            [uncomplicate.neanderthal.random :as random]
            [uncomplicate.neanderthal.internal.api :refer [data-accessor]]))

(def ^:const MAX-SIZE 1000)

(defn- create-uniform [factory size]
  (with-release [real (random/rand-uniform! 0 MAX-SIZE (vctr native-double size))]
    (transfer! real (vctr factory size))))

(defn create-random-vector [factory size]
  (condp = (entry-type (data-accessor factory))
    Float/TYPE (random/rand-uniform! 0 MAX-SIZE (vctr factory size))
    Double/TYPE (random/rand-uniform! 0 MAX-SIZE (vctr factory size))
    Integer/TYPE (create-uniform native-int size)
    Long/TYPE (create-uniform native-long size)
    float (random/rand-uniform! 0 MAX-SIZE (vctr factory size))
    double (random/rand-uniform! 0 MAX-SIZE (vctr factory size))
    int (create-uniform native-int size)
    long (create-uniform native-long size)))

(defn packed-items ^long [^long n]
  (inc (long (/ (* (dec n) (+ 2 n)) 2))))

(defn banded-items ^long [^long n ^long kl ^long ku]
  (* (+ kl ku 1) n))

(defn tridiagonal-items ^long [^long n]
  (max 0 (+ n (* (dec n) 2))))

(defn bidiagonal-items ^long [^long n]
  (+ n (max 0 (dec n))))
