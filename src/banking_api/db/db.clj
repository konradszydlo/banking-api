(ns banking-api.db.db
  (:require [banking-api.db.migration :as migration]
            [banking-api.db.exceptions :as db-exceptions]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [honey.sql.helpers :as helpers :refer [insert-into values where returning]]
            [next.jdbc.result-set :refer [as-unqualified-kebab-maps]])
  (:import
    [com.zaxxer.hikari HikariDataSource]))

(defn- select-one [datasource sql]
  (when-let [results (jdbc/execute-one! datasource sql {:builder-fn as-unqualified-kebab-maps})]
    results))

(defn select-many [datasource sql]
  (when-let [results (jdbc/execute! datasource sql {:builder-fn as-unqualified-kebab-maps})]
    results))

(defn find-by-id [db table id]
  (when-let [result (select-one db (sql/format {:select [:account_number :name :balance]
                                            :from       [table]
                                            :where      [:= :account_number id]}))]
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
  (when-let [result (find-by-id db table id)]
    (execute db (deposit-sql table id (update-in data [:amount] #(+ % (:balance result)))))
    (find-by-id db table id)))

(defn withdraw-money [db table id data]
  (when-let [{:keys [balance]} (find-by-id db table id)]
    ;;; The resulting balance should not fall below zero.
    (if (>= balance (:amount data))
      (do
        (execute db (deposit-sql table id {:amount (- balance (:amount data))}))
        (find-by-id db table id))
      (db-exceptions/throw-insufficient-money-to-withdraw-exception (db-exceptions/insufficient-balance-to-withdraw id)))))

(defn find-two-accounts [db table id-one id-two]
  (when-let [result (select-many db (sql/format {:select [:account_number :balance]
                                                 :from       [table]
                                                 :where      [:in :account_number [id-one id-two 40]]}))]
    result))

(defn send-money [db table id data]
  ;;; You cannot transfer money from an account to itself.
  (if (not= id (:account-number data))
    (when-let [accounts (find-two-accounts db table id (:account-number data))]
      ;;; You can transfer money from one existing account to another existing account.
      (if (> (count accounts) 1)
        (let [sender-account (first (filter #(= id (:account-number %)) accounts))]
          ;;; The resulting balance of the sending account should not fall below zero.
          (if (>= (- (:balance sender-account) (:amount data)) 0)
            ;;; TODO in sql transaction
            (do
              (withdraw-money db table id data)
              (deposit-money db table (:account-number data) data)
              (find-by-id db table id))
            (db-exceptions/throw-insufficient-money-to-send-exception (db-exceptions/insufficient-balance-to-withdraw id))))
        (db-exceptions/throw-cannot-send-to-non-existing-account-exception (db-exceptions/account-needs-to-exist))))
    (db-exceptions/throw-cannot-send-to-itself-exception (db-exceptions/account-cannot-send-to-itself id))))

(comment
  (find-by-id (banking-api.system/get-db) :account 1)
  (create-account (banking-api.system/get-db) :account {:name "Mr. Rabbit"})
  (deposit-money (banking-api.system/get-db) :account 1 {:amount 150})
  (withdraw-money (banking-api.system/get-db) :account 1 {:amount 5} )
  (find-two-accounts (banking-api.system/get-db) :account 202 1)
  (send-money (banking-api.system/get-db) :account 1
              {:account-number 202
               :amount  1})
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
