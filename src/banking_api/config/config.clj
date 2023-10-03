(ns banking-api.config.config
  (:require [banking-api.config.spec :as spec]
            [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]
            [malli.util :as mu]))

(defn apply-defaults
  [config]
  (m/decode spec/Config config mt/default-value-transformer))

(defn validate
  [config]
  (-> (mu/closed-schema spec/Config)
      (m/explain config)
      (me/humanize)))
