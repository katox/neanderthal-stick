;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.core-test
  (:require [midje.sweet :refer [fact facts throws => roughly truthy]]
            [midje.experimental :refer [for-all]]
            [clojure.test.check.generators :as gen]
            [uncomplicate.commons.core :refer [release with-release info]]
            [neanderthal-stick.generator :refer :all]
            [neanderthal-stick.test-util :refer :all]
            [neanderthal-stick.core :refer :all]))

(defn vector-create-describe-test [factory]
  (for-all
    [vctr-desc (gen-vector-descriptor factory)]
    {:num-tests 100}
    (fact "vector can be created from valid descriptor and its descriptor should be the same."
          (with-release [a (create factory vctr-desc)]
            (= (describe a) vctr-desc) => true))))

(defn matrix-create-describe-test [factory]
  (for-all
    [matrix-desc (gen-matrix-descriptor factory)]
    {:num-tests 1000}
    (fact "matrix  can be created from valid descriptor and its descriptor should be the same."
          (with-release [a (create factory matrix-desc)]
            (= (describe a) matrix-desc) => true))))