;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.opencl-integration-test
  (:require [midje.sweet :refer [fact facts =>]]
            [uncomplicate.commons.core :refer [with-release]]
            [uncomplicate.clojurecl.core :as clojurecl :refer [*context* *command-queue*]]
            [uncomplicate.neanderthal.core :as core]
            [uncomplicate.neanderthal.native :refer [dge]]
            [uncomplicate.neanderthal.opencl :refer [with-default-engine opencl-double]]
            [neanderthal-stick.experimental :as exp]
            [neanderthal-stick.opencl])
  (:import (java.io File)))

(defn- temp-file ^File [prefix suffix]
       (doto (File/createTempFile prefix suffix)
             (.deleteOnExit)))

(facts "README OpenCL integration examples"

       (fact "an OpenCL matrix can be saved and loaded back into default host memory"
             (let [file (temp-file "neanderthal-stick-opencl-ge-" ".bin")
                   data (double-array (map double (range 1 7)))]
                  (clojurecl/with-default-1
                    (with-default-engine
                      (let [factory (opencl-double *context* *command-queue*)]
                           (with-release [clge (core/ge factory 3 2 data {:layout :row})]
                                         (exp/save-to-file! clge (.getPath file))))))

                  (with-release [expected (dge 3 2 data {:layout :row})
                                 loaded (exp/load-from-file! (.getPath file))]
                                (= expected loaded) => true)))

       (fact "an OpenCL-saved matrix can be loaded with an explicitly supplied OpenCL factory"
             (let [file (temp-file "neanderthal-stick-opencl-supplied-factory-" ".bin")
                   data (double-array (map double (range 1 7)))]
                  (clojurecl/with-default-1
                    (with-default-engine
                      (let [factory (opencl-double *context* *command-queue*)]
                           (with-release [source (core/ge factory 3 2 data {:layout :row})]
                                         (exp/save-to-file! source (.getPath file)))
                           (with-release [loaded (exp/load-from-file! factory (.getPath file))
                                          host-copy (dge 3 2 {:layout :row})
                                          expected (dge 3 2 data {:layout :row})]
                                         (core/transfer! loaded host-copy)
                                         (= expected host-copy) => true)))))))
