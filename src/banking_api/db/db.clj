(ns banking-api.db.db
  (:require [banking-api.db.migration :as migration]
            [next.jdbc.connection :as connection])
  (:import
    [com.zaxxer.hikari HikariDataSource]))

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
