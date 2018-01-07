#!/usr/bin/env clojure

(require '[clojure.java.jdbc :as j]
         '[clojure.string :as string])

(def db-spec {:connection-uri "jdbc:hsqldb:file:testdb"})

(defn run-sql-script [sql-file]
  (doseq [sql (-> (slurp sql-file) (string/split #";"))]
    (j/execute! db-spec (string/trim sql))))

(run-sql-script "resources/db/hsqldb/schema.sql")
(run-sql-script "resources/db/hsqldb/data.sql")
