(ns banking-api.core-test
  (:require [banking-api.core :as sut]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [expectations.clojure.test :as t :refer [defexpect expect expecting]]
            [ring.mock.request :as mock]))

(defn- parse-json
  [body]
  (cond
    (string? body) (json/parse-string body keyword)
    (some? body) (json/parse-stream (io/reader body) keyword)))

(defn- test-handler
  [request]
  (let [handler sut/app
        response (handler request)]
    (update response :body parse-json)))

(defexpect find-account-by-id
  (expecting
    "find an account"
    (let [{:keys [status body] :as response} (-> (mock/request :get "/account/1")
                                                 (test-handler))]
      (expect {:account-number 1 :balance 11 :name "account 1"} body))))
