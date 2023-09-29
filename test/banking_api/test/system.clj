(ns banking-api.test.system
  (:require [donut.system :as ds]
            [banking-api.system]))

(defn start []
  (ds/start :test))

(defn stop [system]
  (ds/stop system))
