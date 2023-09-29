(ns banking-api.system
  (:require [banking-api.db.db :as db]
            [banking-api.router :as router]
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

(defn ^:private load-config
  [environment]
  {:secrets        {:db {:dbtype   "postgresql"
                         :dbname   "banking_test"
                         :host     "localhost"
                         :port     5432
                         :username "banking"
                         :password "api"}}
   :runtime-config {:db    {:migration-locations ["db/migration/postgresql/schema" "db/migration/postgresql/fixtures"]}
                    :jetty {:port 3000}}})

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