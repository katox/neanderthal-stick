;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.internal.common
  (:require [uncomplicate.neanderthal.core :as core]
            [uncomplicate.neanderthal.internal.api :as api])
  (:import (uncomplicate.neanderthal.internal.api LayoutNavigator DataAccessor Region)))

(defn entry-type-kw
  "Get the (keywordized) data type name stored in `x`."
  [x]
  (let [class ^Class (.entryType (api/data-accessor x))]
    (keyword (.. class getSimpleName toLowerCase))))

(defn native-factory?
  "True iff this `factory` is a native (host) factory."
  [factory]
  (identical? factory (api/native-factory factory)))

(defn layout
  "Get layout ordering of `x` (`:column` or `:row`)."
  [x]
  (if (.isColumnMajor ^LayoutNavigator (api/navigator x)) :column :row))

(defn entry-compatible?
  "True iff the factory of `native-x` and the native factory of `y` can handle the same data type."
  [native-x y]
  (api/compatible? (api/factory native-x) (api/native-factory y)))

(defn options-omit-data?
  "True if the `options` map specifies the data omission during save / freeze and create / thaw."
  [options]
  (boolean (:omit-data options)))

(defn region-options [x]
  (let [reg ^Region (api/region x)]
    {:uplo (api/dec-property (.uplo reg))
     :diag (if (.isDiagUnit reg) :unit :non-unit)
     :kl   (.kl reg)
     :ku   (.ku reg)}))

(defn describe-common [x]
  {:kind :matrix
   :entry-type  (entry-type-kw x)
   :matrix-type (core/matrix-type x)
   :n           (core/ncols x)
   :options     {:layout (layout x)}})
