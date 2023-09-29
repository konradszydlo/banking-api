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

(defn ^:private parse-json
  [body]
  (cond
    (string? body) (json/parse-string body keyword)
    (some? body) (json/parse-stream (io/reader body) keyword)))

(defn app-config []
  (get-in fixtures/*test-system* [::ds/instances :app-config]))

(defn test-handler
  [request]
  (let [handler
        (banking-router/ring-handler (app-config))
        response (handler request)]
    (update response :body parse-json)))

(defexpect find-account-by-id-test
  (expecting
    "find an account"
    (let [{:keys [status body] :as response} (-> (mock/request :get "/account/1")
                                                 (test-handler))]
      (expect 200 status)
      (expect {:account-number 1 :balance 10 :name "First account"} body))))