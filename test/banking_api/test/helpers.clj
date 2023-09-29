(ns banking-api.test.helpers
  (:require [banking-api.test.fixtures :as fixtures]
            [clojure.test :as test]))

(def before-all (test/join-fixtures [fixtures/with-test-system]))
(def before-each (test/join-fixtures [fixtures/with-rollback]))
