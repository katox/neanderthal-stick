;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.composite-test
  (:require [midje.sweet :refer [facts throws =>]]
            [uncomplicate.neanderthal.native :refer [native-float native-double native-int native-long]]
            [neanderthal-stick.core]
            [neanderthal-stick.nippy-ext]
            [neanderthal-stick.core-test :as core-test]
            [neanderthal-stick.nippy-ext-test :as nippy-ext-test]))

(core-test/vector-create-describe-test native-int)
(core-test/vector-create-describe-test native-long)
(core-test/vector-create-describe-test native-float)
(core-test/vector-create-describe-test native-double)

(core-test/matrix-create-describe-test native-float)
(core-test/matrix-create-describe-test native-double)

(nippy-ext-test/vector-round-trip-test native-int)
(nippy-ext-test/vector-round-trip-test native-long)
(nippy-ext-test/vector-round-trip-test native-float)
(nippy-ext-test/vector-round-trip-test native-double)
(nippy-ext-test/subvector-round-trip-test native-int)
(nippy-ext-test/subvector-round-trip-test native-long)
(nippy-ext-test/subvector-round-trip-test native-float)
(nippy-ext-test/subvector-round-trip-test native-double)

(nippy-ext-test/ge-round-trip-test native-double)
(nippy-ext-test/ge-round-trip-test native-float)
(nippy-ext-test/submatrix-ge-round-trip-test native-double)
(nippy-ext-test/submatrix-ge-round-trip-test native-float)

(nippy-ext-test/tr-round-trip-test native-double)
(nippy-ext-test/tr-round-trip-test native-float)
(nippy-ext-test/sy-round-trip-test native-double)
(nippy-ext-test/sy-round-trip-test native-float)

(nippy-ext-test/gb-round-trip-test native-double)
(nippy-ext-test/gb-round-trip-test native-float)

(nippy-ext-test/sb-round-trip-test native-double)
(nippy-ext-test/sb-round-trip-test native-float)

(nippy-ext-test/tb-round-trip-test native-double)
(nippy-ext-test/tb-round-trip-test native-float)

; packed matrix types don't support submatrices
(nippy-ext-test/tp-round-trip-test native-double)
(nippy-ext-test/tp-round-trip-test native-float)
(nippy-ext-test/sp-round-trip-test native-double)
(nippy-ext-test/sp-round-trip-test native-float)

(nippy-ext-test/gd-round-trip-test native-double)
(nippy-ext-test/gd-round-trip-test native-float)
(nippy-ext-test/submatrix-gd-round-trip-test native-double)
(nippy-ext-test/submatrix-gd-round-trip-test native-float)

(nippy-ext-test/gt-round-trip-test native-double)
(nippy-ext-test/gt-round-trip-test native-float)
(nippy-ext-test/submatrix-gt-round-trip-test native-double)
(nippy-ext-test/submatrix-gt-round-trip-test native-float)

(nippy-ext-test/dt-round-trip-test native-double)
(nippy-ext-test/dt-round-trip-test native-float)
(nippy-ext-test/submatrix-dt-round-trip-test native-double)
(nippy-ext-test/submatrix-dt-round-trip-test native-float)

(nippy-ext-test/st-round-trip-test native-double)
(nippy-ext-test/st-round-trip-test native-float)
(nippy-ext-test/submatrix-st-round-trip-test native-double)
(nippy-ext-test/submatrix-st-round-trip-test native-float)
