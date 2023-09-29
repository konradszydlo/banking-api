(ns banking-api.db.exceptions)

(defn insufficient-balance-to-withdraw
  [id]
  {:message (str "Insufficient amount of money for account " id)})

(defn throw-insufficient-money-to-withdraw-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :type :api) cause)))

(defn throw-insufficient-money-to-send-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :type :api) cause)))

(defn account-cannot-send-to-itself [id]
  {:message (format "Account '%s' cannot send money to itself" id)})

(defn throw-cannot-send-to-itself-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :type :api) cause)))

(defn account-needs-to-exist []
  {:message "Cannot transfer money as not all accounts exist"})

(defn throw-cannot-send-to-non-existing-account-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :type :api) cause)))