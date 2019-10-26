;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns ^{:author "Kamil Toman"}
  neanderthal-stick.core
  (:require [uncomplicate.commons.core :refer [info let-release release with-release]]
            [uncomplicate.commons.utils :refer [dragan-says-ex]]
            [uncomplicate.neanderthal.core :as core :refer [transfer!]]
            [uncomplicate.neanderthal.native :refer [factory-by-type]]
            [uncomplicate.neanderthal.internal.api :as api]
            [neanderthal-stick.internal.common :as common]
            [neanderthal-stick.internal.buffer])
  (:import (uncomplicate.neanderthal.internal.host.buffer_block RealBlockVector IntegerBlockVector
                                                                RealGEMatrix RealUploMatrix RealBandedMatrix
                                                                RealPackedMatrix RealDiagonalMatrix)))

(defprotocol ContainerInfo
  (describe [this] "Get the description map of container parameters that can be used to re-create the vector/matrix."))

(extend-protocol ContainerInfo
  IntegerBlockVector
  (describe [x]
    {:entry-type (common/entry-type-kw x) :n (core/dim x)})
  RealBlockVector
  (describe [x]
    {:entry-type (common/entry-type-kw x) :n (core/dim x)})
  RealGEMatrix
  (describe [x]
    (-> (common/describe-common x) (assoc :m (core/mrows x))))
  RealUploMatrix
  (describe [x]
    (let [{:keys [matrix-type] :as dx} (common/describe-common x)
          {:keys [uplo diag]} (common/region-options x)]
      (case matrix-type
        :tr (update dx :options #(merge % {:uplo uplo :diag diag}))
        :tp (update dx :options #(merge % {:uplo uplo :diag diag}))
        :sp (update dx :options #(merge % {:uplo uplo}))
        :sy (update dx :options #(merge % {:uplo uplo}))
        (dragan-says-ex (format "%s is not a valid matrix type. Please send a bug report." matrix-type)
                              {:type matrix-type}))))
  RealBandedMatrix
  (describe [x]
    (let [{:keys [matrix-type] :as dx} (common/describe-common x)
          {:keys [uplo diag kl ku] :as reg} (common/region-options x)]
      (case matrix-type
        :sb (-> dx
                (assoc :k (if (api/options-lower? reg) kl ku))
                (update :options #(merge % {:uplo uplo})))
        :tb (-> dx
                (assoc :k (if (api/options-lower? reg) kl ku))
                (update :options #(merge % {:uplo uplo :diag diag})))
        :gb (-> dx
                (assoc :m (core/mrows x)
                       :kl kl
                       :ku ku))
        (dragan-says-ex (format "%s is not a valid matrix type. Please send a bug report." matrix-type)
                              {:type matrix-type}))))
  RealPackedMatrix
  (describe [x]
    (let [{:keys [matrix-type] :as dx} (common/describe-common x)
          {:keys [uplo diag]} (common/region-options x)]
      (case matrix-type
        :tp (update dx :options #(merge % {:uplo uplo :diag diag}))
        :sp (update dx :options #(merge % {:uplo uplo}))
        (dragan-says-ex (format "%s is not a valid matrix type. Please send a bug report." matrix-type)
                              {:type matrix-type}))))
  RealDiagonalMatrix
  (describe [x]
    (dissoc (common/describe-common x) :options)))

(defn- create-vector [real-factory descriptor source]
  (let [{:keys [n]} descriptor]
    (if source
      (let-release [x (api/create-vector real-factory n false)]
        (transfer! source x))
      (api/create-vector real-factory n true))))

(defn- create-matrix [real-factory descriptor source]
  (let [{:keys [matrix-type m n k kl ku options]} descriptor]
    (case matrix-type
      :ge (core/ge real-factory m n source options)
      :sy (core/sy real-factory n source options)
      :tr (core/tr real-factory n source options)
      :gb (core/gb real-factory m n kl ku source options)
      :sb (core/sb real-factory n k source options)
      :tb (core/tb real-factory n k source options)
      :sp (core/sp real-factory n source options)
      :tp (core/tp real-factory n source options)
      :gt (core/gt real-factory n source options)
      :gd (core/gd real-factory n source options)
      :dt (core/dt real-factory n source options)
      :st (core/st real-factory n source options)
      (dragan-says-ex (format "%s is not a valid matrix type. Please send a bug report." matrix-type)
                      {:type matrix-type}))))

(defn create
  "Create a matrix or a vector using `factory` with parameters given by the `descriptor` map
   and content data `source`. The `source` can be the data source used to read the `descriptor`
   of the structure. If the `factory` is nil the default factory is inferred from the entry data type.
  `ext-options` can specify save/load options external to the data structure itself (for example
  data omission)."
  ([factory descriptor source ext-options]
   (let [{:keys [entry-type]} descriptor
         inferred-factory (factory-by-type entry-type)
         real-factory (or factory inferred-factory)
         real-source (when-not (common/options-omit-data? ext-options) source)]
     (if (common/entry-compatible? inferred-factory real-factory)
       (if (contains? descriptor :matrix-type)
         (create-matrix real-factory descriptor real-source)
         (create-vector real-factory descriptor real-source))
       (dragan-says-ex "You must provide a compatible factory for this data input." {:entry-type entry-type}))))
  ([factory descriptor source]
   (create factory descriptor source nil))
  ([factory descriptor]
   (create factory descriptor nil))
  ([descriptor]
   (create nil descriptor)))
