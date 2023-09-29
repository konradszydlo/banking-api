(defproject banking-api "0.1.0-SNAPSHOT"
  :description "Example Banking API"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.seancorfield/honeysql "2.4.1066"]
                 [com.github.seancorfield/next.jdbc "1.3.894"]
                 [com.zaxxer/HikariCP "5.0.1"]
                 [club.donutpower/system "0.0.165"]
                 [fi.metosin/reitit "0.7.0-alpha6"]
                 [metosin/malli "0.13.0"]
                 [metosin/jsonista "0.3.7"]
                 [metosin/malli "0.13.0"]
                 [metosin/ring-swagger-ui "5.0.0-alpha.0"]
                 [org.flywaydb/flyway-core "9.22.2"]
                 [org.postgresql/postgresql "42.6.0"]
                 [ring/ring-jetty-adapter "1.9.6"]]
  :main ^:skip-aot banking-api.core
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {:dev {:dependencies [[com.github.seancorfield/expectations "2.0.165"]
                                  [lambdaisland/kaocha "1.86.1355"]
                                  [ring/ring-mock "0.4.0"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"tdd" ["run" "-m" "kaocha.runner" "--watch"]
            "kaocha" ["run" "-m" "kaocha.runner"]}
  )