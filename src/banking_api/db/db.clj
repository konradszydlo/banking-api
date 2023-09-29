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

(defn add-audit-sql [table data]
  (-> (insert-into table)
      (values [data])
      sql/format))

(defn add-credit-audit [table data amount description]
  (add-audit-sql table (merge data {:amount amount :description description :operation "credit"})))

(defn add-debit-audit [table data amount description]
  (add-audit-sql table (merge data {:amount amount :description description :operation "debit"})))


(defn deposit-sql [table id data]
  (when-let [result (-> (helpers/update table)
                        (helpers/set {:balance (:amount data)})
                        (where [:= :account_number id])
                        sql/format)]
    result))

(defn deposit-money [db table id data]
  (when-let [result (find-by-id db table id)]
    (execute db (deposit-sql table id (update-in data [:amount] #(+ % (:balance result)))))
    (execute db (add-credit-audit :audit {:account_number id} (:amount data) "deposit"))
    (find-by-id db table id)))

(defn move-money-to [db table id-from id-to data]
  (when-let [result (find-by-id db table id-from)]
    (execute db (deposit-sql table id-from (update-in data [:amount] #(+ % (:balance result)))))
    (execute db (add-credit-audit :audit {:account_number id-from} (:amount data) (str "receive from #" id-to)))
    (find-by-id db table id-from)))

(defn withdraw-money [db table id data]
  (when-let [{:keys [balance]} (find-by-id db table id)]
    ;;; The resulting balance should not fall below zero.
    (if (>= balance (:amount data))
      (do
        (execute db (deposit-sql table id {:amount (- balance (:amount data))}))
        (execute db (add-debit-audit :audit {:account_number id} (:amount data) "withdraw"))
        (find-by-id db table id))
      (db-exceptions/throw-insufficient-money-to-withdraw-exception (db-exceptions/insufficient-balance-to-withdraw id)))))

(defn move-money-from [db table id data]
  (when-let [{:keys [balance]} (find-by-id db table id)]
    ;;; The resulting balance should not fall below zero.
    (if (>= balance (:amount data))
      (do
        (execute db (deposit-sql table id {:amount (- balance (:amount data))}))
        (execute db (add-debit-audit :audit {:account_number id} (:amount data) (str "send to #" (:account-number data)) ))
        (find-by-id db table id)))))

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
            (jdbc/with-transaction [tx db]
              (move-money-from db table id data)
              (move-money-to db table (:account-number data) id data)
              (find-by-id db table id))
            (db-exceptions/throw-insufficient-money-to-send-exception (db-exceptions/insufficient-balance-to-withdraw id))))
        (db-exceptions/throw-cannot-send-to-non-existing-account-exception (db-exceptions/account-needs-to-exist))))
    (db-exceptions/throw-cannot-send-to-itself-exception (db-exceptions/account-cannot-send-to-itself id))))

(defn find-audits-for-account-by-id [db table id]
  (when-let [result (select-many db (sql/format {:select [:amount :description :operation]
                                                 :from        [table]
                                                 :where       [:= :account_number id]}))]
    (sort-by :sequence > (map-indexed (fn [idx item]
                                        (into {}
                                              (-> (if (= "credit" (:operation item))
                                                    (assoc item :credit (:amount item))
                                                    (assoc item :debit (:amount item)))
                                                  (assoc :sequence idx)
                                                  (dissoc :amount :operation))))
                                      result))))

(comment
  (find-by-id (banking-api.system/get-db) :account 1)
  (create-account (banking-api.system/get-db) :account {:name "Mr. Rabbit"})
  (deposit-money (banking-api.system/get-db) :account 1 {:amount 150})
  (withdraw-money (banking-api.system/get-db) :account 1 {:amount 5} )
  (find-two-accounts (banking-api.system/get-db) :account 202 1)
  (send-money (banking-api.system/get-db) :account 1
              {:account-number 202
               :amount  1})
  (find-audits-for-account-by-id (banking-api.system/get-db) :audit 1)
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
