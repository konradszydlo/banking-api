(ns banking-api.router
  (:require [malli.util :as mu]
            [muuntaja.core :as m]
            [reitit.coercion.malli]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.malli]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.spec :as spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.adapter.jetty :as jetty])
  (:import
    [org.eclipse.jetty.server Server]))

(def ^:private min-account-number "should be at least 1")

(def ^:private AccountId [:map [:id [:int {:min 1 :error/message min-account-number}]]])

(def account-id AccountId)

(def ^:private AccountResponse
  [:map
   [:account-number pos-int?]
   [:name :string]
   [:balance int?]])

(defn ^:private routes
  []
  [["/swagger.json"
    {:get {:no-doc true
           :swagger {:info {:title "banking-api"
                            :description "swagger docs with [malli](https://github.com/metosin/malli) and reitit-ring"
                            :version "0.0.1"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/account"
    {:tags #{"account"}}
    ["/:id" {:parameters {:path account-id}}
     [""
      {:get {:summary   "get account details"
             :responses {200 {:body AccountResponse}}
             :handler   (fn [{{{:keys [id]} :path} :parameters}]
                          {:status 200
                           :body {:account-number id
                                  :balance        (+ 10 id)
                                  :name (str "account " id)}})}}]]]])

(defn ^:private router
  []
  (ring/router
    (routes)
    {:validate spec/validate ;; enable spec validation for route data
     :exception pretty/exception
     :data {:coercion (reitit.coercion.malli/create
                        {;; set of keys to include in error messages
                         :error-keys #{#_:type :coercion :in :schema :value :errors :humanized #_:transformed}
                         ;; schema identity function (default: close all map schemas)
                         :compile mu/closed-schema
                         ;; strip-extra-keys (effects only predefined transformers)
                         :strip-extra-keys true
                         ;; add/set default values
                         :default-values true
                         ;; malli options
                         :options nil})
            :muuntaja m/instance
            :middleware [;; swagger
                         swagger/swagger-feature
                         ;; query-params & form-params
                         parameters/parameters-middleware
                         ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                         ;; encoding response body
                         muuntaja/format-response-middleware
                         ;; exception handling
                         exception/exception-middleware
                         ;; decoding request body
                         muuntaja/format-request-middleware
                         ;; coercing response bodys
                         coercion/coerce-response-middleware
                         ;; coercing request parameters
                         coercion/coerce-request-middleware]}}))

(defn ring-handler
  []
  (ring/ring-handler (router)
                     (ring/routes
                       (swagger-ui/create-swagger-ui-handler
                         {:path "/"
                          :config {:validatorUrl nil
                                   :urls [{:name "swagger", :url "swagger.json"}]
                                   :operationsSorter "alpha"}})
                       (ring/create-default-handler))))

(defn start
  [{{:keys [jetty]} :runtime-config :as app-config}]
  (jetty/run-jetty
    (ring-handler)
    (merge jetty {:allow-null-path-info true
                  :send-server-version? false
                  :send-date-header? false
                  :join? false}))) ;; false so that we can stop it at the repl!

(defn stop
  [^Server server]
  (.stop server) ; stop is async
  (.join server) ; so let's make sure it's really stopped!
  )
