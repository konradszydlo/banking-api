(ns banking-api.db.exceptions)

(defn insufficient-balance-to-withdraw
  [id]
  {:message (str "Insufficient amount of money for account " id)})

(defn throw-insufficient-money-to-withdraw-exception
  [{:keys [message data cause] :as error}]
  (throw (ex-info message (assoc data :type :api) cause)))