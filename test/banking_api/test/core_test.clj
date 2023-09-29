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
        "Deposit money"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/deposit")
                                               {:amount 100})
                                 (test-handler))]
          (expect {:name "Mr. Black" :balance 100} (dissoc body :account-number)))))))

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
        "Withdraw money"
        (let [{:keys [body]} (-> (post-request (str "/account/" (:account-number body) "/withdraw")
                                               {:amount 5})
                                 (test-handler))]
          (expect {:name "Mr. Black" :balance 95} (dissoc body :account-number))))
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