(defproject botsad "0.1.0-SNAPSHOT"
  :description "Botanic garden web scraping"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [dk.ative/docjure "1.14.0"]
                 [reaver "0.1.3"]]
  :main ^:skip-aot botsad.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})

