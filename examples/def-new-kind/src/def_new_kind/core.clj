(ns def-new-kind.core
  (:require [uncomplicate.commons.core :refer [let-release release]]
            [uncomplicate.commons.utils :refer [dragan-says-ex]]
            [uncomplicate.neanderthal.core :as core :refer [transfer!]]
            [uncomplicate.neanderthal.native :refer [native-double dge dgd]]
            [neanderthal-stick.core :as stick :refer [ContainerInfo describe]]
            [neanderthal-stick.experimental :as exp]
            [neanderthal-stick.internal.common :as common])
  (:import (uncomplicate.neanderthal.internal.api Matrix)
           (uncomplicate.commons.core Releaseable)
           (java.io DataInput DataOutput Writer PrintWriter)))

(defprotocol Transformer
  (left [this] "Left transforming matrix")
  (right [this] "Right transforming matrix")
  (transpose [this] "Transpose this transformer")
  (mul [this a] "Multiply `a` by this matrix transformer, i.e. `(L a R)`."))

(deftype MTransformer [^Matrix l ^Matrix r]
  ; define accessors and core operations
  Transformer
  (left [_]
    l)
  (right [_]
    r)
  (transpose [_]
    (MTransformer. (core/trans r) (core/trans l)))
  (mul [_ a]
    (core/mm l a r))
  ; define the structure description for persistence
  ContainerInfo
  (describe [_]
    {:kind       ::Transformer
     :entry-type (common/entry-type-kw l)
     :left       (describe l)
     :right      (describe r)})
  ; we need to be able to release used resources like a good citizen
  Releaseable
  (release [_]
    (release l)
    (release r))
  ; provide standard java collection contracts
  Object
  (hashCode [_]
    (-> (hash ::MTransformer) (hash-combine l) (hash-combine r)))
  (equals [_ b]
    (and (instance? MTransformer b)
         (= l (left b))
         (= r (right b)))))

(defn create-transformer
  "A Transformer constructor with the entry type check."
  [left right]
  (let [le-type (common/entry-type-kw left)
        re-type (common/entry-type-kw right)]
    (if (and (= le-type re-type))
      (->MTransformer left right)
      (dragan-says-ex "Left and right matrices need to have the same entry type." {:entry-type-left  le-type
                                                                                   :entry-type-right re-type}))))

; redefine the default print method to see inspect the content
(defmethod print-method MTransformer [a ^Writer w]
  (let [p (PrintWriter. w)
        separator ^String (apply str (repeat 70 \-))]
    (.println p (str \# (.getSimpleName (class a)) "[mxn:" (core/mrows (left a)) "x" (core/ncols (right a)) "]"))
    (.println p separator)
    (print-method (left a) p)
    (print-method (right a) p)
    (.println p separator)))

; define the constructor (from the description) for the new structure kind
(defmethod stick/create* ::Transformer
  [factory descriptor _]
  (let-release [left (stick/create factory (:left descriptor))
                right (stick/create factory (:right descriptor))]
    (create-transformer left right)))

; define a transfer to an output stream (save)
(defmethod transfer! [MTransformer DataOutput]
  [^MTransformer source ^DataOutput destination]
  (transfer! (left source) destination)
  (transfer! (right source) destination)
  destination)

; define a transfer from an output stream (load)
(defmethod transfer! [DataInput MTransformer]
  [^DataInput source ^MTransformer destination]
  (transfer! source (left destination))
  (transfer! source (right destination))
  destination)

(comment

  ;Sample Usage
  ;============

  ; define a new transformer
  (def mt (create-transformer (dge 3 2 [1 0
                                        0 1
                                        1 0] {:layout :row})
                              (dge 2 4 [1 0 1 0
                                        0 1 0 1] {:layout :row})))
  ;=> #'def-new-kind.core/mt

  mt
  ;=> #MTransformer[mxn:3x4]
  ;----------------------------------------------------------------------
  ;#RealGEMatrix[double, mxn:3x2, layout:row, offset:0]
  ;▤       ↓       ↓       ┓
  ;→       1.00    0.00
  ;→       0.00    1.00
  ;→       1.00    0.00
  ;┗                       ┛
  ;#RealGEMatrix[double, mxn:2x4, layout:row, offset:0]
  ;▤       ↓       ↓       ↓       ↓       ┓
  ;→       1.00    0.00    1.00    0.00
  ;→       0.00    1.00    0.00    1.00
  ;┗                                       ┛
  ;----------------------------------------------------------------------

  ; some test data (ones on the diagonal, zeros elsewhere)
  (def A (dgd 2 (repeat 1.0)))
  ;=> #'def-new-kind.core/A

  (mul mt A)
  ;=> #RealGEMatrix[double, mxn:3x4, layout:column, offset:0]
  ;▥       ↓       ↓       ↓       ↓       ┓
  ;→       1.00    0.00    1.00    0.00
  ;→       0.00    1.00    0.00    1.00
  ;→       1.00    0.00    1.00    0.00
  ;┗                                       ┛

  ; save and load it back

  (exp/save-to-file! mt "/tmp/mt.bin")
  ;=> nil

  (def reloaded-mt (exp/load-from-file! "/tmp/mt.bin"))
  ;=> #'def-new-kind.core/reloaded-mt

  ; test we got the same thing
  (= (mul mt A)
     (mul reloaded-mt A)
     (dge 3 4 [1 0 1 0
               0 1 0 1
               1 0 1 0] {:layout :row}))
  ;=> true

  ; save the transformer with a different internal representation
  (exp/save-to-file! (transpose mt) "/tmp/mt_trans.bin")
  ;=> nil

  ; load it back
  (def reloaded-mt-trans (exp/load-from-file! "/tmp/mt_trans.bin"))
  ;=> #'def-new-kind.core/reloaded-mt-trans

  ; and test again
  (= (mul (transpose mt) A)
     (mul reloaded-mt-trans A)
     (dge 4 3 [1 0 1
               0 1 0
               1 0 1
               0 1 0] {:layout :row}))
  ;=> true

  )