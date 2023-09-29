(ns banking-api.db.db
  (:require [banking-api.db.migration :as migration]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [honey.sql.helpers :as helpers :refer [insert-into values where returning]]
            [next.jdbc.result-set :refer [as-unqualified-kebab-maps]])
  (:import
    [com.zaxxer.hikari HikariDataSource]))

(defn- select [datasource sql]
  (when-let [results (jdbc/execute-one! datasource sql {:builder-fn as-unqualified-kebab-maps})]
    results))

(defn find-by-id [db table id]
  (when-let [result (select db (sql/format {:select [:account_number :name :balance]
                                            :from [table]
                                            :where [:= :account_number id]}))]
    result))

(defn- create-sql [table account]
  (-> (insert-into table)
      (values [account])
      (returning :account_number :name :balance)
      sql/format))

(defn- execute [datasource sql]
  (when-let [results (jdbc/execute-one! datasource sql {:builder-fn as-unqualified-kebab-maps})]
    results))

(defn create-account [db table data]
  (when-let [result (execute db (create-sql table data))]
    result))

(defn deposit-sql [table id data]
  (when-let [result (-> (helpers/update table)
                        (helpers/set {:balance (:amount data)})
                        (where [:= :account_number id])
                        sql/format)]
    result))

(defn deposit-money [db table id data]
  (when-let [result (execute db (deposit-sql table id data))]
    (find-by-id db table id)))

(comment
  (find-by-id (banking-api.system/get-db) :account 1)
  (create-account (banking-api.system/get-db) :account {:name "Mr. Rabbit"})
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
