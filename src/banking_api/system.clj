(ns banking-api.system
  (:require [aero.core :refer [read-config]]
            [banking-api.config.config :as config]
            [banking-api.db.db :as db]
            [banking-api.router :as router]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [donut.system :as ds]))

(def system-atom (atom []))

(defn get-db []
  (get-in @system-atom [::ds/instances :app-config :db]))

(def ^:private base-system
  {::ds/defs
   {:env {}
    :app-config {:db #::ds{:start (fn [{:keys [::ds/config]}] (db/start config))
                           :post-start (fn [{:keys [::ds/instance ::ds/config]}] (db/post-start instance config))
                           :stop (fn [{:keys [::ds/instance]}] (db/stop instance))
                           :config {:db (ds/ref [:env :secrets :db])
                                    :runtime-config (ds/ref [:env :runtime-config])}}
                 :router #::ds{:start (fn [{:keys [::ds/config]}] (router/start config))
                               :stop (fn [{:keys [::ds/instance]}] (router/stop instance))
                               :config {:db (ds/ref [:app-config :db])
                                        :runtime-config (ds/ref [:env :runtime-config])}}}}})

(defn ^:private validate-config
  [config-file config]
  (log/infof "Validating '%s' to make sure it's correct" config-file)
  (if-let [errors (config/validate config)]
    (let [message (format "Bad '%s'! Errors are '%s'." config-file errors)]
      (throw (ex-info message {:message message :data {:message message :config-file config-file :errors errors}})))
    config))

(defn ^:private load-config
  [environment]
  (let [config-file (str "config/config" (when-not (= :production environment) (str "-" (name environment))) ".edn")]
    (log/infof "Loading config file '%s'." config-file)
    (->> (io/resource config-file)
         (read-config)
         (config/apply-defaults)
         (validate-config config-file))))

(defmethod ds/named-system :base
  [_]
  base-system)

(defmethod ds/named-system :local
  [_]
  (ds/system :base {[:env] (load-config :local)}))

(defmethod ds/named-system :test
  [_]
  (ds/system :base {[:env] (load-config :test)
                    [:app-config :router] ::disabled
                    [:app-config :runtime-config] (ds/ref [:env :runtime-config])}))