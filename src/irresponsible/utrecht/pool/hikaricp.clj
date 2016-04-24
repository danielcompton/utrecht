(ns irresponsible.utrecht.pool.hikaricp
  (:require [irresponsible.utrecht :refer [Pool]]
            [clojure.java.jdbc :as j]
            [hikari-cp.core :as h])
  (:import [com.zaxxer.hikari HikariDataSource]))

(def default-config
  {:auto-commit false})

(defn hikaricp
  "Given a HikariCP configuration map, creates a pool
   args: [conf]
     conf should be a map of parameters as accepted by hikari
   returns: Pool"
  [conf]
  (h/make-datasource (merge default-config conf)))

(extend-type HikariDataSource
  Pool
  (get-conn [pool]
    (j/get-connection {:datasource pool})))
