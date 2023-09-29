(ns banking-api.core
  {:author "Konrad Szydlo"}
  (:require [banking-api.router :as router])
  (:gen-class))

(defn start []
  (router/start {:jetty {:port 3000}})
  (println "server running in port 3000"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
