;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.cuda
  (:require [uncomplicate.commons.utils :refer [dragan-says-ex]]
            [uncomplicate.neanderthal.core :as core]
            [uncomplicate.neanderthal.cuda]
            [neanderthal-stick.core :refer [ContainerInfo describe]]
            [neanderthal-stick.internal.common :as common])
  (:import (uncomplicate.neanderthal.internal.cpp.cuda.structures
             CUBlockVector CUGEMatrix CUUploMatrix)))

(extend-protocol ContainerInfo
  CUBlockVector
  (describe [x]
    {:kind :vector :entry-type (common/entry-type-kw x) :n (core/dim x)})
  CUGEMatrix
  (describe [x]
    (-> (common/describe-common x) (assoc :m (core/mrows x))))
  CUUploMatrix
  (describe [x]
    (let [{:keys [matrix-type] :as dx} (common/describe-common x)
          {:keys [uplo diag]} (common/region-options x)]
      (case matrix-type
        :tr (update dx :options #(merge % {:uplo uplo :diag diag}))
        :tp (update dx :options #(merge % {:uplo uplo :diag diag}))
        :sp (update dx :options #(merge % {:uplo uplo}))
        :sy (update dx :options #(merge % {:uplo uplo}))
        (dragan-says-ex (format "%s is not a valid matrix type. Please send a bug report." matrix-type)
                        {:type matrix-type})))))
