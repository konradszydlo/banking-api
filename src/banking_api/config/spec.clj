(ns banking-api.config.spec)

(def Config
  [:map
   [:secrets [:map
              [:db
               [:map
                [:dbtype {:default "postgresql"} [:= "postgresql"]]
                [:dbname {:default "bankingdb"} [:or [:= "bankingdb"] [:= "banking_test"]]]
                [:host {:default "localhost"} :string]
                [:port {:default 5432} pos-int?]
                [:username :string]
                [:password :string]]]]]
   [:runtime-config [:map
                     [:db [:map [:migration-locations {:default ["db/migration/postgresql"]} [:vector :string]]]]
                     [:environment {:default :local} [:enum :local :test]]
                     [:jetty {:default 8080} [:map [:port pos-int?]]]]]])
