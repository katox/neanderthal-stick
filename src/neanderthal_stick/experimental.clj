;;   Copyright (c) Kamil Toman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) or later
;;   which can be found in the file LICENSE at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns neanderthal-stick.experimental
  (:require [clojure.java.io :as io]
            [taoensso.nippy :as nippy]
            [uncomplicate.commons.core :refer [let-release]]
            [uncomplicate.neanderthal.core :refer [transfer!]]
            [neanderthal-stick.core :refer [describe create]]
            [neanderthal-stick.internal.common :as common])
  (:import (java.io DataOutputStream DataInputStream)))

(defn save!
  "Save the Neanderthal matrix or the vector `x` to the `data-out` stream.
  `ext-options` can specify save options external to the data structure itself (for example data omission)."
  ([x data-out ext-options]
   (if (common/options-omit-data? ext-options)
     (nippy/freeze-to-out! data-out (assoc-in (describe x) [:ext-options :omit-data] true))
     (do
       (nippy/freeze-to-out! data-out (describe x))
       (transfer! x data-out))))
  ([x data-out]
   (save! data-out x nil)))

(defn save-to-file!
  "Save the Neanderthal matrix or the vector `x` to the file path name `file-path`. Opens `file-path as data output stream,
   writes the structure and its contents, closes `file-path`. `ext-options` are passed to `save!`."
  ([x file-path ext-options]
   (with-open [out (DataOutputStream. (io/output-stream (io/file file-path)))]
     (save! x out ext-options))
   nil)
  ([x f]
   (save-to-file! x f nil)))

(defn load!
  "Load a Neanderthal matrix or a vector from the `data-in` stream using the `factory` for construction.
   If no `factory` is provided the default factory is inferred from the entry data type.
  `ext-options` can specify load options external to the data structure itself (for example data omission)."
  ([factory data-in ext-options]
   (let [descriptor (nippy/thaw-from-in! data-in)
         options (merge (:ext-options descriptor) ext-options)]
     (let-release [x (create factory descriptor nil)]
       (when-not (common/options-omit-data? options)
         (transfer! data-in x))
       x)))
  ([factory data-in]
   (load! factory data-in nil))
  ([data-in]
   (load! nil data-in nil)))

(defn load-from-file!
  "Load a Neanderthal matrix or a vector from the file path name `file-path` using the `factory` for construction.
   Opens `file-path with as data input stream, reads the structure and its contents, closes `file-path`.
   If no `factory` is provided the default factory is inferred from the entry data type.
  `ext-options` are passed to `load!`."
  ([factory file-path ext-options]
   (with-open [in (DataInputStream. (io/input-stream (io/file file-path)))]
     (load! factory in ext-options)))
  ([factory f]
   (load-from-file! factory f nil))
  ([f]
   (load-from-file! nil f nil)))
