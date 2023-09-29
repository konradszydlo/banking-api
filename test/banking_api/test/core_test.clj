(ns banking-api.test.core-test
  (:require [banking-api.router :as banking-router]
            [banking-api.test.fixtures :as fixtures]
            [banking-api.test.helpers :as th]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [donut.system :as ds]
            [expectations.clojure.test :as t :refer [defexpect expect expecting use-fixtures]]
            [ring.mock.request :as mock]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)

(defn- parse-json
  [body]
  (cond
    (string? body) (json/parse-string body keyword)
    (some? body) (json/parse-stream (io/reader body) keyword)))

(defn- app-config []
  (get-in fixtures/*test-system* [::ds/instances :app-config]))

(defn- test-handler
  [request]
  (let [handler
        (banking-router/ring-handler (app-config))
        response (handler request)]
    (update response :body parse-json)))

(defn- post-request [url payload]
  (-> (mock/request :post url)
      (mock/json-body payload)
      (assoc :app-config (app-config))))

(defexpect find-account-by-id-test
  (expecting
    "find an account"
    (let [{:keys [status body] :as response} (-> (mock/request :get "/account/1")
                                                 (test-handler))]
      (expect 200 status)
      (expect {:account-number 1 :balance 10 :name "First account"} body))))

(defexpect create-account-test
  (expecting
    "Create an account"
    (let [{:keys [status body]} (-> (post-request "/account" {:name "Mr. Black"})
                                    (test-handler))]
      (expect 201 status)
      (expect {:name "Mr. Black" :balance 0} (dissoc body :account-number)))))

(defexpect deposit-money-test
  (expecting
    "Create an account"
    (let [{:keys [body]} (-> (post-request "/account" {:name "Mr. Black"})
                             (test-handler))]
      (expect {:name "Mr. Black" :balance 0} (dissoc body :account-number))
      (expecting
        "Allow to deposit only positive amount of money"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/deposit")
                                               {:amount -100})
                                 (test-handler))]
          (expect "should be a positive int" (:message (first (:errors body))))))
      (expecting
        "Deposit money to new account"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/deposit")
                                               {:amount 100})
                                 (test-handler))]
          (expect {:name "Mr. Black" :balance 100} (dissoc body :account-number))))
      (expecting
        "Deposit money to account with existing balance"
        (let [{:keys [body]} (-> (post-request (str "/account/1/deposit")
                                               {:amount 100})
                                 (test-handler))]
          (expect {:name "First account" :balance 110} (dissoc body :account-number)))))))

(defexpect withdraw-money-test
  (expecting
    "Create an account"
    (let [{:keys [body]} (-> (post-request "/account" {:name "Mr. Black"})
                                    (test-handler))]
      (expect {:name "Mr. Black" :balance 0} (dissoc body :account-number))
      (expecting
        "Deposit money"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/deposit")
                                               {:amount 100})
                                 (test-handler))]
          (expect {:name "Mr. Black" :balance 100} (dissoc body :account-number))))
      (expecting
        "Withdraw money from new account"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/withdraw")
                                               {:amount 5})
                                 (test-handler))]
          (expect {:name "Mr. Black" :balance 95} (dissoc body :account-number))))
      (expecting
        "Withdraw money from existing account"
        (let [{:keys [body]} (-> (post-request (str "/account/1/withdraw")
                                               {:amount 5})
                                 (test-handler))]
          (expect {:name "First account" :balance 5} (dissoc body :account-number))))
      (expecting
        "Try to withdraw negative amount of money"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/withdraw")
                                               {:amount -5})
                                 (test-handler))]
          (expect "should be a positive int" (:message (first (:errors body))))))
      (expecting
        "Try to withdraw zero amount of money"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/withdraw")
                                               {:amount 0})
                                 (test-handler))]
          (expect "should be a positive int" (:message (first (:errors body))))))
      (expecting
        "Try to withdraw more money than in current balance"
        (let [{:keys [status]} (-> (post-request (str "/account/" (:account-number body) "/withdraw")
                                               {:amount 100})
                                 (test-handler))]
          (expect 500 status))))))

(defexpect transfer-money-test
  (expecting
    "Create an account"
    (let [{:keys [body]} (-> (post-request "/account" {:name "Mr. Black"})
                                    (test-handler))]
      (expect {:name "Mr. Black" :balance 0} (dissoc body :account-number))
      (expecting
        "Deposit money"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/deposit")
                                               {:amount 100})
                                 (test-handler))]
          (expect {:name "Mr. Black" :balance 100} (dissoc body :account-number))))
      (expecting
        "Transfer money from new account to first/seeded account"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/send")
                                               {:amount 40
                                                :account-number 1})
                                 (test-handler))]
          (expect {:name "Mr. Black" :balance 60} (dissoc body :account-number))
          (expecting
            "find sender account"
            (let [{:keys [status body]} (-> (mock/request :get "/account/1")
                                                         (test-handler))]
              (expect {:account-number 1 :balance 50 :name "First account"} body)))
          (expecting
            "find receiver account"
            (let [{:keys [status body]} (-> (mock/request :get (str "/account/" (:account-number body)))
                                            (test-handler))]
              (expect {:balance 60 :name "Mr. Black"} (dissoc body :account-number))))))
      (expecting
        "sending more than current balance"
        (let [{:keys [status]} (-> (post-request (str "/account/" (:account-number body) "/send")
                                                 {:amount         200
                                                  :account-number 1})
                                   (test-handler))]
          (expect 500 status)))
      (expecting
        "One account does not exist"
        (let [{:keys [status]} (-> (post-request (str "/account/" (:account-number body) "/send")
                                                 {:amount         200
                                                  :account-number 999999999999})
                                   (test-handler))]
          (expect 500 status)))
      (expecting
        "Sending to itself"
        (let [{:keys [status]} (-> (post-request "/account/1/send"
                                                 {:amount         200
                                                  :account-number 1})
                                   (test-handler))]
          (expect 500 status))))))

(defexpect audit-account-test
  (expecting
    "Create an account"
    (let [new-account-response (-> (post-request "/account" {:name "Mr. Black"})
                                   (test-handler))
          new-account-number (:account-number (:body new-account-response))
          second-new-account-response (-> (post-request "/account" {:name "Mr. White"})
                                          (test-handler))
          second-new-account-number (:account-number (:body second-new-account-response))]
      (expect {:name "Mr. Black" :balance 0} (dissoc (:body new-account-response) :account-number))
      (expect {:name "Mr. White" :balance 0} (dissoc (:body second-new-account-response) :account-number))
      ;;; 1. deposit $100 to account #1
      (expecting
        "Deposit money"
        (let [{:keys [body]} (-> (post-request (str "/account/1/deposit")
                                               {:amount 100})
                                 (test-handler))]
          (expect {:name "First account" :balance 110} (dissoc body :account-number))))
      ;;; 2. transfer $5 from account #1 to account #900
      (expecting
        "Transfer money from first/seeded account to new account"
        (let [{:keys [body]} (-> (post-request (str "/account/1/send")
                                               {:amount 5
                                                :account-number new-account-number})
                                 (test-handler))]
          (expect {:name "First account" :balance 105} (dissoc body :account-number))))
      ;;;; 3. transfer $10 from account #800 to account #1
      (expecting
        "Deposit money in second new account"
        (let [{:keys [body]} (-> (post-request (str "/account/" second-new-account-number "/deposit")
                                               {:amount 20})
                                 (test-handler))]
          (expect {:name "Mr. White" :balance 20} (dissoc body :account-number))))
      (expecting
        "Transfer money from second new account to first/seeded"
        (let [{:keys [body]} (-> (post-request (str "/account/" second-new-account-number "/send")
                                               {:amount 10
                                                :account-number 1})
                                 (test-handler))]
          (expect {:name "Mr. White" :balance 10} (dissoc body :account-number))))
      ;;;; 4. withdraw $20 from account #1
      (expecting
        "Withdraw money from first/seeded"
        (let [{:keys [body]} (-> (post-request (str "/account/1/withdraw")
                                               {:amount 20})
                                 (test-handler))]
          (expect {:name "First account" :balance 95} (dissoc body :account-number))))
      (expecting
        "Access audit log for first/seeded account"
        (let [{:keys [status body] :as response} (-> (mock/request :get "/account/1/audit")
                                                     (test-handler))]
          (expect
            [{:sequence 3
              :debit 20
              :description "withdraw"}
             {:sequence    2
              :credit      10
              :description (str "receive from #" second-new-account-number)}
             {:sequence    1
              :debit       5
              :description (str "send to #" new-account-number)
              }
             {:sequence 0
              :credit 100
              :description "deposit"}]
            body)))
      (expecting
        "Access audit log for new account"
        (let [{:keys [body] :as response} (-> (mock/request :get (str "/account/" new-account-number "/audit"))
                                                     (test-handler))]
          (expect
            [{:sequence 0
              :credit 5
              :description "receive from #1"}]
            body)))
      (expecting
        "Access audit log for second new account"
        (let [{:keys [body] :as response} (-> (mock/request :get (str "/account/" second-new-account-number "/audit"))
                                                     (test-handler))]
          (expect
            [{:sequence 1
              :debit 10
              :description "send to #1"}
             {:sequence 0
              :credit 20
              :description "deposit"}]
            body))))))
