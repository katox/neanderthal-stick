;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.cuda-integration-test
  (:require [midje.sweet :refer [fact facts =>]]
            [taoensso.nippy :as nippy]
            [uncomplicate.commons.core :refer [release with-release]]
            [uncomplicate.clojurecuda.core :as clojurecuda :refer [current-context default-stream]]
            [uncomplicate.neanderthal.core :as core]
            [uncomplicate.neanderthal.cuda :refer [cuda-double]]
            [uncomplicate.neanderthal.native :refer [dge dv]]
            [neanderthal-stick.experimental :as exp]
            [neanderthal-stick.nippy-ext :refer [with-real-factory]]
            [neanderthal-stick.cuda])
  (:import (java.io File)))

(defn- temp-file ^File [prefix suffix]
  (doto (File/createTempFile prefix suffix)
    (.deleteOnExit)))

(facts "README CUDA integration examples"

       (fact "a host-saved matrix can be loaded with a CUDA factory"
             (let [file (temp-file "neanderthal-stick-cuda-ge-" ".bin")
                   data (double-array (map double (range 1 7)))]
               (with-release [source (dge 3 2 data {:layout :row})]
                 (exp/save-to-file! source (.getPath file)))

               (clojurecuda/with-default
                 (let [factory (cuda-double (current-context) default-stream)]
                   (with-release [cuge (exp/load-from-file! factory (.getPath file))
                                  host-copy (dge 3 2 {:layout :row})
                                  expected (dge 3 2 data {:layout :row})]
                     (core/transfer! cuge host-copy)
                     (= expected host-copy) => true)))))

       (fact "Nippy thaw can use a CUDA real factory"
             (let [frozen (nippy/freeze {:single (dv 1.0)
                                         :double (dv 1.0 2.0)
                                         :hexa   (dge 3 2 (range 1 7) {:layout :row})})]
               (clojurecuda/with-default
                 (with-real-factory (cuda-double (current-context) default-stream)
                                    (let [{:keys [single double hexa]} (nippy/thaw frozen)]
                                      (try
                                        (with-release [single-host (dv 1)
                                                       double-host (dv 2)
                                                       hexa-host (dge 3 2 {:layout :row})
                                                       expected-single (dv 1.0)
                                                       expected-double (dv 1.0 2.0)
                                                       expected-hexa (dge 3 2 (range 1 7) {:layout :row})]
                                          (core/transfer! single single-host)
                                          (core/transfer! double double-host)
                                          (core/transfer! hexa hexa-host)
                                          (= expected-single single-host) => true
                                          (= expected-double double-host) => true
                                          (= expected-hexa hexa-host) => true)
                                        (finally
                                          (release single)
                                          (release double)
                                          (release hexa)))))))))