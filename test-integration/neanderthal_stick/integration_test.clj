;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.integration-test
  (:require [clojure.java.io :as io]
            [midje.sweet :refer [fact facts throws =>]]
            [taoensso.nippy :as nippy]
            [uncomplicate.commons.core :refer [release with-release]]
            [uncomplicate.neanderthal.core :as core :refer [transfer!]]
            [uncomplicate.neanderthal.native :refer [native-double dv dge dtr]]
            [neanderthal-stick.core :as stick]
            [neanderthal-stick.experimental :as exp]
            [neanderthal-stick.nippy-ext :refer [with-real-factory]])
  (:import (clojure.lang ExceptionInfo)
           (java.io DataInputStream DataOutputStream File)))

(defn- temp-file ^File [prefix suffix]
  (doto (File/createTempFile prefix suffix)
    (.deleteOnExit)))

(defn- sample-doubles ^doubles []
  (double-array (map double (range 1 13))))

(facts "experimental save/load examples"

       (fact "save-to-file! and load-from-file! round-trip a native vector"
             (let [file (temp-file "neanderthal-stick-vector-" ".bin")]
               (with-release [source (transfer! (sample-doubles) (dv 12))
                              loaded (exp/load-from-file!
                                       (do
                                         (exp/save-to-file! source (.getPath file))
                                         (.getPath file)))]
                 (= source loaded) => true)))

       (fact "load-from-file! accepts a supplied factory"
             (let [file (temp-file "neanderthal-stick-supplied-factory-" ".bin")]
               (with-release [source (dge 3 4 (sample-doubles) {:layout :row})
                              loaded (exp/load-from-file!
                                       native-double
                                       (do
                                         (exp/save-to-file! source (.getPath file))
                                         (.getPath file)))]
                 (= source loaded) => true
                 (= (stick/describe source) (stick/describe loaded)) => true)))

       (fact "save! and load! work with caller-supplied streams"
             (let [file (temp-file "neanderthal-stick-streams-" ".bin")]
               (with-release [source (dge 3 2 (range 1 7) {:layout :row})]
                 (with-open [out (DataOutputStream. (io/output-stream file))]
                   (exp/save! source out))
                 (with-open [in (DataInputStream. (io/input-stream file))]
                   (with-release [loaded (exp/load! native-double in)]
                     (= source loaded) => true))))))

(facts "split-saving examples"

       (fact "omit-data saves only the descriptor and loads an empty structure"
             (let [file (temp-file "neanderthal-stick-desc-only-" ".bin")]
               (with-release [source (dge 3 2 (range 1 7) {:layout :row})
                              loaded (exp/load-from-file!
                                       (do
                                         (exp/save-to-file! source (.getPath file) {:omit-data true})
                                         (.getPath file)))]
                 (stick/describe source) => {:entry-type :double
                                             :kind :matrix
                                             :matrix-type :ge
                                             :n 2
                                             :m 3
                                             :options {:layout :row}}
                 (= (stick/describe source) (stick/describe loaded)) => true
                 (= source loaded) => false)))

       (fact "split-saved descriptor can be populated by transferring data separately"
             (let [desc-file (temp-file "neanderthal-stick-desc-" ".bin")
                   data-file (temp-file "neanderthal-stick-data-" ".bin")]
               (with-release [source (dge 3 2 (range 1 7) {:layout :row})]
                 (exp/save-to-file! source (.getPath desc-file) {:omit-data true})
                 (with-open [out-data (DataOutputStream. (io/output-stream data-file))]
                   (transfer! source out-data))
                 (with-release [loaded (exp/load-from-file! (.getPath desc-file))]
                   (with-open [in-data (DataInputStream. (io/input-stream data-file))]
                     (transfer! in-data loaded))
                   (= source loaded) => true)))))

(facts "saving views/submatrices examples"

       (fact "a non-contiguous submatrix cannot be saved directly"
             (let [file (temp-file "neanderthal-stick-small-view-" ".bin")]
               (with-release [source (dge 4 4 (range 1 17) {:layout :column})
                              view (core/submatrix source 0 0 3 2)]
                 (exp/save-to-file! view (.getPath file)) => (throws ExceptionInfo))))

       (fact "a dense copy of a submatrix can be saved and loaded"
             (let [file (temp-file "neanderthal-stick-small-copy-" ".bin")]
               (with-release [source (dge 4 4 (range 1 17) {:layout :column})
                              view (core/submatrix source 0 0 3 2)
                              copy (dge 3 2 view)
                              loaded (exp/load-from-file!
                                       (do
                                         (exp/save-to-file! copy (.getPath file))
                                         (.getPath file)))]
                 (= copy loaded) => true))))

(facts "Nippy examples"

       (fact "Nippy extension freezes and thaws a triangular matrix"
             (with-release [source (dtr 10 (range 50) {:diag :unit})
                            loaded (nippy/thaw (nippy/freeze source))]
               (= source loaded) => true))

       (fact "Nippy extension handles composite structures"
             (let [frozen (nippy/freeze {:single (dv 1.0)
                                         :double (dv 1.0 2.0)
                                         :hexa   (dge 3 2 (range 1 7) {:layout :row})})]
               (let [{:keys [single double hexa] :as loaded} (nippy/thaw frozen)]
                 (try
                   (= #{:single :double :hexa} (set (keys loaded))) => true
                   (= single (dv 1.0)) => true
                   (= double (dv 1.0 2.0)) => true
                   (= hexa (dge 3 2 (range 1 7) {:layout :row})) => true
                   (finally
                     (release single)
                     (release double)
                     (release hexa))))))

       (fact "Nippy thaw can use an explicitly supplied real factory"
             (let [frozen (nippy/freeze (dv 1.0 2.0 3.0))]
               (with-real-factory native-double
                                  (with-release [loaded (nippy/thaw frozen)]
                                    (= loaded (dv 1.0 2.0 3.0)) => true)))))
