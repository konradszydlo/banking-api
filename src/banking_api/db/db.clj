(ns banking-api.db.db
  (:require [banking-api.db.migration :as migration]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [next.jdbc.result-set :refer [as-unqualified-kebab-maps]])
  (:import
    [com.zaxxer.hikari HikariDataSource]))

(defn select [datasource sql]
  (when-let [results (jdbc/execute-one! datasource sql {:builder-fn as-unqualified-kebab-maps})]
    results))

(defn find-by-id [db table id]
  (when-let [result (select db (sql/format {:select [:account_number :name :balance]
                                            :from [table]
                                            :where [:= :account_number id]}))]
    result))

(comment
  (find-by-id (banking-api.system/get-db) :account 1)
  )


;; DONUT LIFECYCLE FUNCTIONS ↓

(defn start ^HikariDataSource
  [{:keys [db] :as config}]
  (connection/->pool HikariDataSource db))

(defn post-start
  [^HikariDataSource datasource config]
  (let [{{{:keys [migration-locations]} :db} :runtime-config} config]
    (migration/migrate datasource migration-locations)))

(defn stop
  [^HikariDataSource datasource]
  (.close datasource))
