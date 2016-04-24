(ns irresponsible.utrecht.pool.hikaricp
  (:require [irresponsible.utrecht :as u]
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
  u/Pool
  (get-conn [pool]
    (j/get-connection {:datasource pool}))
  (transact [pool lock-mode isolation func]
    (let [read-only? (case lock-mode :ro true :rw false)]
      (when-not (u/isolations isolation)
        (-> (str "transact: invalid isolation: " isolation)
            (ex-info {:got isolation :valid u/isolations}) throw))
      (j/db-transaction* {:datasource pool} func {:isolation isolation  :read-only? read-only?}))))
