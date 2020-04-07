# neanderthal-stick

Save/Load Extensions for [Neanderthal](https://neanderthal.uncomplicate.org/)

## Motivation

Neanderthal is a very fast clojure matrix library but if you need to run longer
computations it might not be enough to keep everything in memory.
Often you need to save some results, resume when ready and continue from
that point on. Or you might want to free up some memory but you don't want to
discard anything computed so far.

That's where **neanderthal-stick** comes to help. It allows you to take your
vector or a matrix and *stick it* to somewhere - a disk file, a network
stream, somewhere **durable.**

The API is simple yet flexible enough to support some perks Neanderthal gives
you for free - like automatically working with your GPU just by swapping
a factory.

## Getting started

Add the necessary dependency to your project:

```$clj
    [neanderthal-stick "0.2.0]
```

## Usage

The basic usage is just to
[`save!`](https://katox.github.io/neanderthal-stick/neanderthal-stick.experimental.html#var-save)
something to a stream and
[`load!`](https://katox.github.io/neanderthal-stick/neanderthal-stick.experimental.html#var-load)
it back later. That's it!.

For reference documentation refer to the project
[github pages](https://katox.github.io/neanderthal-stick/index.html).

*Note*: The API is very early stage and subject to change thus it is
in the `expertimental` namespace.

### Saving to File

If the final destination is a standard file and you only need to save or load
one thing you can use the convenience functions `save-to-file!` and
`load-from-file!` respectively to handle opening and closing the file for you.

```$clj
(require '[uncomplicate.commons.core :refer [let-release with-release release]]
         '[uncomplicate.neanderthal.native :refer [native-double dv dge dtr]]
         '[uncomplicate.neanderthal.core :as core :refer [transfer!]]
         '[neanderthal-stick.core :as stick]
         '[neanderthal-stick.experimental :as exp])

; setup an array of 100 million one-by-one increasing numbers
(def src-vctr ^doubles (double-array (long 1e8)))
(dotimes [i (int 1e8)]
  (aset ^doubles src-vctr i (double (inc i))))

; and fill a proper neanderthal vector
(def dnative (transfer! src-vctr (dv (long 1e8))))

(exp/save-to-file! dnative "/tmp/dnative.bin")
;=> nil

(exp/load-from-file! "/tmp/dnative.bin")
;=> #RealBlockVector[double, n:100000000, offset: 0, stride:1]
;[1.0     2.0     3.0        ⋯   1.00E+8 1.00E+8 ]
```

### Saving and Loading With Supplied Factory

If you need to handle vectors or matrices that reside in the GPU memory
you have to require the proper namespace (`opencl` or `cuda`) to let
it initialize.

When saving there is no other setup to do:
```$clj
(require '[uncomplicate.clojurecl.core :as clojurecl :refer [*context* *command-queue* finish!]]
         '[uncomplicate.neanderthal.opencl :refer [with-default-engine opencl-double]]
         '[neanderthal-stick.opencl])

; use ancient OpenCL 1.x to be safe
(clojurecl/with-default-1
  (with-default-engine
    (let [factory (opencl-double *context* *command-queue*)]
      (with-release [clge (core/ge factory 10000 10000 src-vctr)]
        (exp/save-to-file! clge "/tmp/ge.bin")))))
;=> nil
```

and you can load the saved structure normally:

```$clj
(exp/load-from-file! "/tmp/ge.bin")
;=> #RealGEMatrix[double, mxn:10000x10000, layout:column, offset:0]
;   ▥       ↓       ↓       ↓       ↓       ↓       ┓    
;   →    1.0     1.00E+4    ⁙    1.00E+8 1.00E+8         
;   →    2.0     1.00E+4    ⁙    1.00E+8 1.00E+8         
;   →       ⁙       ⁙       ⁙       ⁙       ⁙            
;   →    1.00E+4 2.00E+4    ⁙    1.00E+8 1.00E+8         
;   →    1.00E+4 2.00E+4    ⁙    1.00E+8 1.00E+8         
;   ┗                                               ┛    
```
Note that you should handle loaded objects the same way you normally
do with neanderthal structures. For instance you should `release`
the `ge` matrix above to free the associated direct buffers
(outside the JVM memory heap).

Loading back to host memory isn't often enough. Neanderthal-stick allows
you to switch contexts as needed (even from OpenCL to CUDA) by providing
a factory to the load function - the factory used when saving isn't
important. The entry data type (`double` or `float`) must match though.

```$clj
(require '[uncomplicate.clojurecuda.core :as clojurecuda :refer [current-context with-context default-stream]]
         '[uncomplicate.neanderthal.cuda :refer [cuda-float cuda-double]]
         '[neanderthal-stick.cuda])

(clojurecuda/with-default
  (let [factory (cuda-double (current-context) default-stream)]
    (with-release [cuge (exp/load-from-file! factory "/tmp/ge.bin")]
      (prn cuge))))
;#CUGEMatrix[double, mxn:10000x10000, layout:column, offset:0]
;   ▥       ↓       ↓       ↓       ↓       ↓       ┓    
;   →    1.0     1.00E+4    ⁙    1.00E+8 1.00E+8         
;   →    2.0     1.00E+4    ⁙    1.00E+8 1.00E+8         
;   →       ⁙       ⁙       ⁙       ⁙       ⁙            
;   →    1.00E+4 2.00E+4    ⁙    1.00E+8 1.00E+8         
;   →    1.00E+4 2.00E+4    ⁙    1.00E+8 1.00E+8         
;   ┗                                               ┛    
```

### Using Nippy

Neanderthal-stick defines *optional* custom type extensions
to [Nippy](https://github.com/ptaoussanis/nippy) for user-facing neanderthal types.

To use it it is sufficient to require Nippy's main namespace as well as the `nippy-ext`
namespace. From that point on you can use it with Nippy as any other built-in Clojure type.

```$clj
(require '[taoensso.nippy :as nippy]
         '[neanderthal-stick.nippy-ext :refer [with-real-factory]])

(def mytr (core/tr native-double 10 (range 50) {:diag :unit}))
(def mytr-frozen-bytes (nippy/freeze mytr))
(def mytr-copy (nippy/thaw mytr-frozen-bytes))

mytr-copy
;=> #RealUploMatrix[double, type:tr, mxn:10x10, layout:column, offset:0]
;   ▥       ↓       ↓       ↓       ↓       ↓       ─    
;   →     ·1·       *       *       *       *       ⋯    
;   →       1.00  ·1·       *       *       *            
;   →       2.00   10.00  ·1·       *       *            
;   →       3.00   11.00   18.00  ·1·       *            
;   →       4.00   12.00   19.00   25.00  ·1·            
;   |       ⋮                                       ⋱    

(= mytr mytr-copy)
;=> true
```

#### Using Nippy With Composite Structures

You can use the Nippy extension including composite structures
with the caveat that you still need to manually`release` neanderthal types
when finished.

```$clj
(def composite {:single (dv 1.0)
                :double (dv 1.0 2.0)
                :hexa   (dge 3 2 (range 1 7) (:layout :row))})
(def frozen-composite (nippy/freeze composite))

(nippy/thaw frozen-composite)
;=> {:single #RealBlockVector[double, n:1, offset: 0, stride:1]
;[   1.00 ]
;, :double #RealBlockVector[double, n:2, offset: 0, stride:1]
;[   1.00    2.00 ]
;, :hexa #RealGEMatrix[double, mxn:3x2, layout:column, offset:0]
;   ▥       ↓       ↓       ┓    
;   →       1.00    4.00         
;   →       2.00    5.00         
;   →       3.00    6.00         
;   ┗                       ┛    
;}
```

#### Using Nippy With Supplied Factory

There is no way how to pass a parameter through the Nippy's serialization engine
so if you need a different factory to create neanderthal types you need to
set a dynamic binding with the help of macro `with-real-factory`.

The same caveat regarding `release` applies. If you really need to
save and load composite types it is probably easier to create a new
type that also implements `uncomplicate.commons.core/Releaseable`
and Nippy's protocols.

```$clj
(clojurecuda/with-default
  (with-real-factory (cuda-double (current-context) default-stream)
      (let [{:keys [single double hexa] :as composite} (nippy/thaw frozen-composite)]
          (prn composite)
          (release single)
          (release double)
          (release hexa))))
;{:single #CUBlockVector[double, n:1, offset:0 stride:1]
;[   1.00 ]
;, :double #CUBlockVector[double, n:2, offset:0 stride:1]
;[   1.00    2.00 ]
;, :hexa #CUGEMatrix[double, mxn:3x2, layout:column, offset:0]
;   ▥       ↓       ↓       ┓    
;   →       1.00    4.00         
;   →       2.00    5.00         
;   →       3.00    6.00         
;   ┗                       ┛    
;}
;=> true
```

### Saving Views / Submatrices

For performance reasons you can only save structures
that provide a dense vector view. The rule of the thumb
is that if its stride is bigger than one you have to make a copy.

```$clj
(with-release [bigm (dge 10000 10000 src-vctr)
               small-view (core/submatrix bigm 3 2)]
  (exp/save-to-file! small-view "/tmp/small_ge_view.bin"))
;Execution error (ExceptionInfo) at uncomplicate.neanderthal.internal.host.buffer_block.RealGEMatrix/view_vctr (buffer_block.clj:848).
;Strided GE matrix cannot be viewed as a dense vector.

(with-release [bigm (dge 10000 10000 src-vctr)
               small-view (core/submatrix bigm 3 2)
               small-copy (dge 3 2 small-view)]
  (exp/save-to-file! small-copy "/tmp/small_ge_view.bin"))
=> nil

(with-release [small-view (exp/load-from-file!"/tmp/small_ge_view.bin" )]
  (prn small-view))
;#RealGEMatrix[double, mxn:3x2, layout:column, offset:0]
;   ▥       ↓       ↓       ┓    
;   →    1.0     1.00E+4         
;   →    2.0     1.00E+4         
;   →    3.0     1.00E+4         
;   ┗                       ┛    
;=> nil
```

### Split-saving

Split-saving is an advanced feature. It allows you save the data descriptor
(shape) into one place and the actual contents into another.

When loading a split-saved structure its empty skeleton is created
(with the correct parameters) and you need to `transfer!` the data
back to it. As a source you can use a different object or even a data
stream. The transmission size is adjusted automatically according
to the destination capacity.

```$clj
(require '[clojure.java.io :as io])
(import '[java.io DataOutputStream DataInputStream])

(def myge (dge 3 2 src-vctr {:layout :row}))
;=> #'user/myge

myge
;=> #RealGEMatrix[double, mxn:3x2, layout:row, offset:0]
;   ▤       ↓       ↓       ┓    
;   →       1.00    2.00         
;   →       3.00    4.00         
;   →       5.00    6.00         
;   ┗                       ┛    

(stick/describe myge)
;=> {:entry-type :double, :matrix-type :ge, :n 2, :m 3, :options {:layout :row}}

(exp/save-to-file! myge "/tmp/ge_desc_only.bin" {:omit-data true})
;=> nil

(with-open [out-data (DataOutputStream. (io/output-stream (io/file "/tmp/data.bin")))]
  (transfer! myge out-data))
;=> #object[java.io.DataOutputStream 0x1379d050 "java.io.DataOutputStream@1379d050"]

(def empty-ge-copy (exp/load-from-file! "/tmp/ge_desc_only.bin"))
;=> #'user/myge-copy

empty-ge-copy
;=> #RealGEMatrix[double, mxn:3x2, layout:row, offset:0]
;   ▤       ↓       ↓       ┓    
;   →       0.00    0.00         
;   →       0.00    0.00         
;   →       0.00    0.00         
;   ┗                       ┛

(with-open [in-data (DataInputStream. (io/input-stream (io/file "/tmp/data.bin")))]
  (transfer! in-data empty-ge-copy))
;=> #RealGEMatrix[double, mxn:3x2, layout:row, offset:0]
;   ▤       ↓       ↓       ┓    
;   →       1.00    2.00         
;   →       3.00    4.00         
;   →       5.00    6.00         
;   ┗                       ┛    
```

### Defining New Structure That Supports Save/Load

If you need to define your own structures (new structure *kinds*)
you can still have it supported by **neanderthal-stick** but it is
a little bit more involved.

There is an example in the repository:
[examples/def-new-kind](https://github.com/katox/neanderthal-stick/tree/master/examples/def-new-kind/).

TLDR; you need to define a new self-description ability, a constructor
from that descriptor and the actual content save and load `transfer!` multimethods.

## License

Copyright © 2019 Kamil Toman

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
