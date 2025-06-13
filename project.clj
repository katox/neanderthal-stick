(defproject neanderthal-stick "0.5.0-SNAPSHOT"
  :description "Save/Load Extensions for Neanderthal, Fast Clojure Matrix Library"
  :url "https://github.com/katox/neanderthal-stick"
  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [uncomplicate/neanderthal "0.53.2"]
                 [org.bytedeco/mkl "2025.0-1.5.11" :classifier "linux-x86_64-redist"]
                 [com.taoensso/nippy "3.5.0"]]

  :codox {:metadata {:doc/format :markdown}
          :src-dir-uri "https://github.com/katox/neanderthal-stick/blob/master/"
          :src-linenum-anchor-prefix "L"
          :namespaces [neanderthal-stick.core
                       neanderthal-stick.buffer
                       neanderthal-stick.experimental
                       neanderthal-stick.nippy-ext]
          :output-path "doc/codox"}

  ;;also replaces lein's default JVM argument TieredStopAtLevel=1
  :jvm-opts ^:replace ["-Dclojure.compiler.direct-linking=true"]

  :repl-options {:init-ns neanderthal-stick.core}

  :profiles {:dev {:plugins [[lein-midje "3.2.1"]
                             [lein-codox "0.10.6"]]
                   :global-vars {*warn-on-reflection* true
                                 *assert* false
                                 *unchecked-math* :warn-on-boxed
                                 *print-length* 128}
                   :dependencies [[midje "1.9.9"]
                                  [org.clojure/test.check "1.0.0"]]}
             :java8 {:jvm-opts ^:replace ["-Dclojure.compiler.direct-linking=true"]}}

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :source-paths ["src"]
  :test-paths ["test"])
