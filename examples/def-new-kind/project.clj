(defproject def-new-kind "0.4.0"
  :description "Save/Load Extensions for Neanderthal, Fast Clojure Matrix Library"
  :url "https://github.com/katox/neanderthal-stick"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [neanderthal-stick "0.4.0"]]
  :repl-options {:init-ns def-new-kind.core}
  :profiles {:java8 {:jvm-opts ^:replace ["-XX:+TieredCompilation" "-XX:TieredStopAtLevel=1"]}}

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :source-paths ["src"])
