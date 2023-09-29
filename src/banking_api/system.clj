(ns banking-api.system
  (:require
    [donut.system :as ds]
    [banking-api.router :as router]))

(def ^:private base-system
  {::ds/defs
   {:env {}
    :app-config {:router #::ds{:start (fn [{:keys [::ds/config]}] (router/start config))
                               :stop (fn [{:keys [::ds/instance]}] (router/stop instance))
                               :config {:runtime-config (ds/ref [:env :runtime-config])}}}}})

(defn ^:private load-config
  [environment]
  {:runtime-config {:jetty {:port 3000}}})

(defmethod ds/named-system :base
  [_]
  base-system)

(defmethod ds/named-system :local
  [_]
  (ds/system :base {[:env] (load-config :local)}))
