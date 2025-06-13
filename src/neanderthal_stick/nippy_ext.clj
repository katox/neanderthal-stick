;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.nippy-ext
  (:require [taoensso.nippy :as nippy]
            [uncomplicate.commons.core :refer [info let-release release with-release]]
            [uncomplicate.commons.utils :refer [dragan-says-ex]]
            [uncomplicate.neanderthal.core :refer [transfer!]]
            [uncomplicate.neanderthal.internal.api :as api]
            [neanderthal-stick.core :refer [describe create]]
            [neanderthal-stick.internal.common :as common]))

(def ^{:dynamic true
       :doc     "Dynamically bound factory that is used in Neanderthal vector and matrix constructors."}
  *neanderthal-factory* nil)

(defmacro with-real-factory
  "Create a bind to use the `factory` during Neanderthal real vector and matrix construction.
  ```
      (clojurecuda/with-default
         (with-open [in (DataInputStream. (io/input-stream (io/file \"/tmp/my_matrix.bin\")))]
             (with-real-factory (cuda-double (current-context) default-stream)
                 (nippy/thaw-from-in! in)))
  ```
  "
  [factory & body]
  `(binding [*neanderthal-factory* ~factory]
     ~@body))

(defn- freeze-to-out!
  "Freeze ContainerInfo `x` and its contents to the `data-output` stream."
  [data-output x]
  (nippy/freeze-to-out! data-output (describe x))
  (transfer! x data-output)
  nil)

(defn- freeze-through-native!
  "Freeze `x` to `data-output` by using a temp copy in host memory if needed."
  [data-output x]
  (let [native-x (api/native x)]
    (try
      (nippy/freeze-to-out! data-output native-x)
      (finally
        (when-not (identical? native-x x)
          (release native-x))))))

(defn- thaw-from-in!
  "Thaw a previously frozen descriptor from the `data-input to a new Neanderthal structure."
  [data-input]
  (let [factory *neanderthal-factory*
        descriptor (nippy/thaw-from-in! data-input)
        ext-options (:ext-options descriptor)
        input (when-not (common/options-omit-data? ext-options) data-input)]
    (if (and factory input (not (common/native-factory? factory)))
      (let [native-x (create descriptor)]
        (let-release [x (api/raw native-x factory)]
          (try
            (transfer! input x)
            (finally
              (release native-x)))))
      (let-release [x (create factory descriptor)]
        (if input
          (transfer! input x)
          x)))))

;; =================== Extend Neanderthal Types with Nippy Protocols ===================

(nippy/extend-freeze uncomplicate.neanderthal.internal.cpp.structures.RealBlockVector
                     :uncomplicate.neanderthal/RealBlockVector
                     [x data-output]
                     (freeze-to-out! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/RealBlockVector
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.cpp.structures.IntegerBlockVector
                     :uncomplicate.neanderthal/IntegerBlockVector
                     [x data-output]
                     (freeze-to-out! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/IntegerBlockVector
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.cpp.structures.RealGEMatrix
                     :uncomplicate.neanderthal/RealGEMatrix
                     [x data-output]
                     (freeze-to-out! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/RealGEMatrix
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.cpp.structures.RealUploMatrix
                     :uncomplicate.neanderthal/RealUploMatrix
                     [x data-output]
                     (freeze-to-out! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/RealUploMatrix
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.cpp.structures.RealBandedMatrix
                     :uncomplicate.neanderthal/RealBandedMatrix
                     [x data-output]
                     (freeze-to-out! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/RealBandedMatrix
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.cpp.structures.RealPackedMatrix
                     :uncomplicate.neanderthal/RealPackedMatrix
                     [x data-output]
                     (freeze-to-out! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/RealPackedMatrix
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.cpp.structures.RealDiagonalMatrix
                     :uncomplicate.neanderthal/RealDiagonalMatrix
                     [x data-output]
                     (freeze-to-out! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/RealDiagonalMatrix
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.api.CUVector
                     :uncomplicate.neanderthal/CUVector
                     [x data-output]
                     (freeze-through-native! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/CUVector
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.api.CUMatrix
                     :uncomplicate.neanderthal/CUMatrix
                     [x data-output]
                     (freeze-through-native! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/CUMatrix
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.api.CLVector
                     :uncomplicate.neanderthal/CLVector
                     [x data-output]
                     (freeze-through-native! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/CLVector
                   [data-input]
                   (thaw-from-in! data-input))

(nippy/extend-freeze uncomplicate.neanderthal.internal.api.CLMatrix
                     :uncomplicate.neanderthal/CLMatrix
                     [x data-output]
                     (freeze-through-native! data-output x))

(nippy/extend-thaw :uncomplicate.neanderthal/CLMatrix
                   [data-input]
                   (thaw-from-in! data-input))
