(defproject banking-api "0.1.0-SNAPSHOT"
  :description "Example Banking API"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [fi.metosin/reitit "0.7.0-alpha6"]
                 [metosin/jsonista "0.3.7"]
                 [ring/ring-jetty-adapter "1.9.6"]]
  :main ^:skip-aot banking-api.core
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[com.github.seancorfield/expectations "2.0.165"]
                                  [lambdaisland/kaocha "1.86.1355"]
                                  [ring/ring-mock "0.4.0"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"tdd" ["run" "-m" "kaocha.runner" "--watch"]
            "kaocha" ["run" "-m" "kaocha.runner"]}
  )