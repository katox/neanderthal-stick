(defproject com.leafclick/neanderthal-stick "0.7.0"
  :description "Save/Load Extensions for Neanderthal, Fast Clojure Matrix Library"
  :url "https://github.com/katox/neanderthal-stick"
  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html"}
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [org.uncomplicate/neanderthal-base "0.63.0"]
                 [org.uncomplicate/neanderthal-openblas "0.63.0"]
                 [com.taoensso/nippy "3.6.0"]]

  :codox {:metadata {:doc/format :markdown}
          :src-dir-uri "https://github.com/katox/neanderthal-stick/blob/master/"
          :src-linenum-anchor-prefix "L"
          :namespaces [neanderthal-stick.core
                       neanderthal-stick.buffer
                       neanderthal-stick.experimental
                       neanderthal-stick.nippy-ext]
          :output-path "doc/codox"}

  ;;also replaces lein's default JVM argument TieredStopAtLevel=1
  :jvm-opts ^:replace ["-Dclojure.compiler.direct-linking=true"
                       "--enable-native-access=ALL-UNNAMED"]

  :repl-options {:init-ns neanderthal-stick.core}

  :profiles {:dev {:plugins [[lein-midje "3.2.1"]
                             [lein-codox "0.10.8"]]
                   :global-vars {*warn-on-reflection* true
                                 *assert* false
                                 *unchecked-math* :warn-on-boxed
                                 *print-length* 128}
                   :dependencies [[midje "1.10.10"]
                                  [org.clojure/test.check "1.1.1"]
                                  [nrepl/nrepl "1.3.1"]]}

             :mkl {:dependencies [[org.uncomplicate/neanderthal-mkl "0.63.0"]
                                  [org.bytedeco/mkl "2025.3-1.5.13" :classifier "linux-x86_64-redist"]
                                  ;[org.bytedeco/mkl "2025.3-1.5.13" :classifier "windows-x86_64-redist"]
                                  ]}

             :opencl {:source-paths ["src-opencl"]
                      :dependencies [[org.uncomplicate/neanderthal-opencl "0.63.0"]]}

             :cuda {:source-paths ["src-cuda"]
                    :dependencies [[org.uncomplicate/neanderthal-cuda "0.63.0"]
                                   [org.bytedeco/cuda-redist "13.1-9.19-1.5.13" :classifier "linux-x86_64"]
                                   [org.bytedeco/cuda-redist-cublas "13.1-9.19-1.5.13" :classifier "linux-x86_64"]
                                   ;[org.bytedeco/cuda-redist "13.1-9.19-1.5.13" :classifier "windows-x86_64"]
                                   ;[org.bytedeco/cuda-redist-cublas "13.1-9.19-1.5.13" :classifier "windows-x86_64"]
                                   ]}

             :integration {:test-paths ["test-integration"]}
             :integration-opencl {:test-paths ["test-integration-opencl"]}
             :integration-cuda {:test-paths ["test-integration-cuda"]}

             :java8 {:jvm-opts ^:replace ["-Dclojure.compiler.direct-linking=true"]}}

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :source-paths ["src"]
  :test-paths ["test"]

  :repositories [["sonatype-snapshots" {:id  "SonatypeSnapshots"
                                        :url "https://oss.sonatype.org/content/repositories/snapshots/"}]
                 ["sonatype-releases" {:id  "SonatypeReleases"
                                       :url "https://oss.sonatype.org/content/repositories/releases/"}]])
