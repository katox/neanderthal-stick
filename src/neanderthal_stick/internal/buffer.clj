;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns ^{:author "Kamil Toman"}
  neanderthal-stick.internal.buffer
  (:require [uncomplicate.commons.core :refer [info with-release]]
            [uncomplicate.commons.utils :refer [dragan-says-ex]]
            [uncomplicate.neanderthal.block :as block]
            [uncomplicate.neanderthal.core :as core :refer [transfer!]]
            [uncomplicate.neanderthal.internal.api :as api]
            [uncomplicate.neanderthal.internal.host.buffer-block])
  (:import (java.io OutputStream InputStream DataOutput DataInput)
           (java.nio ByteBuffer)
           (java.nio.channels Channels)
           (uncomplicate.neanderthal.internal.api RealNativeMatrix VectorSpace Block DataAccessor
                                                  CUVector CUMatrix CLVector CLMatrix)
           (uncomplicate.neanderthal.internal.host.buffer_block IntegerBlockVector RealBlockVector)))


(defn- write-buffer [^OutputStream out ^ByteBuffer native-buffer ^long offset]
  (let [buf (.slice native-buffer)]                         ; BIG ENDIAN buffer view with its own position
    (when (pos? offset)
      (.position buf (int offset)))
    (.write (Channels/newChannel out) buf)))

(defn- read-buffer [^InputStream in ^ByteBuffer native-buffer ^long offset]
  (let [buf (.slice native-buffer)]                         ; BIG ENDIAN buffer view with its own position
    (when (pos? offset)
      (.position buf (int offset)))
    (.read (Channels/newChannel in) buf)))

(defn write-vctr-data!
  "Write contents of the contiguous block vector `source-vctr` to a data stream `out`."
  [^OutputStream out ^Block source-vctr]
  (when (pos? (.dim ^VectorSpace source-vctr))
    (let [offset (.offset source-vctr)
          entry-width (.entryWidth (api/data-accessor source-vctr))
          buf (.buffer source-vctr)]
      (write-buffer out buf (* entry-width offset)))))

(defn read-vctr-data!
  "Read the data stram `in` into the contiguous block vector `destination-vctr`."
  [^InputStream in ^Block destination-vctr]
  (when (pos? (.dim ^VectorSpace destination-vctr))
    (let [offset (.offset destination-vctr)
          entry-width (.entryWidth (api/data-accessor destination-vctr))
          buf (.buffer destination-vctr)]
      (read-buffer in buf (* entry-width offset)))))

(defmethod transfer! [IntegerBlockVector DataOutput]
  [^IntegerBlockVector source ^DataOutput destination]
  (when (pos? (core/dim source))
    (if (block/contiguous? source)
      (write-vctr-data! destination source)
      (dragan-says-ex "You cannot directly transfer from a non-contiguous vector. Make a dense copy first."
                      (info source))))
  destination)

(defmethod transfer! [DataInput IntegerBlockVector]
  [^DataInput source ^IntegerBlockVector destination]
  (when (pos? (core/dim destination))
    (if (block/contiguous? destination)
      (read-vctr-data! source destination)
      (dragan-says-ex "You cannot directly transfer to a non-contiguous vector. Make a dense copy first."
                      (info destination))))
  destination)

(defmethod transfer! [RealBlockVector DataOutput]
  [^RealBlockVector source ^DataOutput destination]
  (when (pos? (core/dim source))
    (if (block/contiguous? source)
      (write-vctr-data! destination source)
      (dragan-says-ex "You cannot directly transfer from a non-contiguous vector. Make a dense copy first."
                      (info source))))
  destination)

(defmethod transfer! [DataInput RealBlockVector]
  [^DataInput source ^RealBlockVector destination]
  (when (pos? (core/dim destination))
    (if (block/contiguous? destination)
      (read-vctr-data! source destination)
      (dragan-says-ex "You cannot directly transfer to a non-contiguous vector. Make a dense copy first."
                      (info destination))))
  destination)

(defmethod transfer! [RealNativeMatrix DataOutput]
  [^RealNativeMatrix source ^DataOutput destination]
  (when (pos? (core/dim source))
    (let [source-view (core/view-vctr source)]
      (if (block/contiguous? source-view)
        (write-vctr-data! destination source-view)
        (dragan-says-ex "You cannot directly transfer from a matrix with a non-contiguous vector view. Make a dense copy first."
                        (info source)))))
  destination)

(defmethod transfer! [DataInput RealNativeMatrix]
  [^DataInput source ^RealNativeMatrix destination]
  (when (pos? (core/dim destination))
    (let [destination-view (core/view-vctr destination)]
      (if (block/contiguous? destination-view)
        (read-vctr-data! source (core/view-vctr destination-view))
        (dragan-says-ex "You cannot directly transfer to a matrix with a non-contiguous vector view. Make a dense copy first."
                        (info destination)))))
  destination)

(defmethod transfer! [CUVector DataOutput]
  [source destination]
  (with-release [h (api/host source)]
    (transfer! h destination)))

(defmethod transfer! [DataInput CUVector]
  [source destination]
  (with-release [h (api/host destination)]
    (transfer! source h)
    (transfer! h destination)))

(defmethod transfer! [CUMatrix DataOutput]
  [source destination]
  (with-release [h (api/host source)]
    (transfer! h destination)))

(defmethod transfer! [DataInput CUMatrix]
  [source destination]
  (with-release [h (api/host destination)]
    (transfer! source h)
    (transfer! h destination)))

(defmethod transfer! [CLVector DataOutput]
  [source destination]
  (with-release [h (api/host source)]
    (transfer! h destination)))

(defmethod transfer! [DataInput CLVector]
  [source destination]
  (with-release [h (api/host destination)]
    (transfer! source h)
    (transfer! h destination)))

(defmethod transfer! [CLMatrix DataOutput]
  [source destination]
  (with-release [h (api/host source)]
    (transfer! h destination)))

(defmethod transfer! [DataInput CLMatrix]
  [source destination]
  (with-release [h (api/host destination)]
    (transfer! source h)
    (transfer! h destination)))
