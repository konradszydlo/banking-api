(ns banking-api.core
  {:author "Konrad Szydlo"}
  (:require [banking-api.system :as system]
            [donut.system :as ds])
  (:gen-class))

(defn start []
  (reset! system/system-atom (ds/start :local))
  (println "server running in port 3000"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
