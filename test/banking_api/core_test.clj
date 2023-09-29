(ns banking-api.core-test
  (:require [expectations.clojure.test :as t :refer [defexpect expect expecting]]
            [banking-api.core :refer :all]))

(defexpect first-test
  (expecting
    "check inc fn"
    (expect 2 (inc 1))))
