(ns irresponsible.utrecht
  (:require [mpg.core :as mpg]
            [clojure.java.jdbc :as j]
            [hikari-cp.core :as h]))

(def setup
  "Installs conversion hooks for postgres datatypes
   args: []
   returns: nil"
  mpg/patch)

(defn make-pool
  "Given a HikariCP configuration map, creates a pool
   Args: [conf]
     conf should be a map of parameters as accepted by hikari
   Returns: pool"
  [conf]
  ;; apply defaults
  {:datasource
   (-> {:adapter "postgresql"
        :port-number 5432
        :server-name "localhost"
        :auto-commit false}
       ;; :connection-timeout 30000
       ;; :validation-timeout 5000
       ;; :idle-timeout       600000
       ;; :max-lifetime       1800000
       ;; :minimum-idle       10
       ;; :maximum-pool-size  10
       ;; :pool-name          "db-pool"       
       ;; :register-mbeans false
       (merge conf)
       h/make-datasource)})

(def close-pool
  "Closes the given pool
   args: [pool]
   returns: nil"
  (comp h/close-datasource :datasource))

(defmacro with-conn
  "[macro] Executes code within the scope of a connection to the database
   args: [[name pool] & exprs]
     name: symbol to bind the connection to in the scope
     pool: pool as returned from make-pool
     writability: one of :ro, :rw. Whether to lock for read only"
  [& exprs]
  `(j/with-db-connection ~@exprs))

(defn prep
  "Prepares a query against the given connection
   args: [conn sql]
   returns: PreparedStatement"
  [{:keys [connection]} sql]
  (j/prepare-statement connection sql))

(defmacro with-prep
  "[macro] Prepares one or more queries against the connection
   args: [conn query-bindings & exprs]
     query-bindings: a vector that looks like assignment. each first item
                     is a symbol to bind the PreparedStatement to and each
                     second item is a sql string"
  [conn names & exprs]
  (when (not= 0 (mod (count names) 2))
    (throw (ex-info "with-prepared assignment vector is uneven" {})))
  (letfn [(f [[name sql]]
            [name `(prep ~conn ~sql)])]
    (let [parts (partition 2 names)
          names (map first parts)
          assigns (vec (mapcat f parts))
          closes (mapv (partial list '.close) names)]
      `(let ~assigns
         (try
           (do ~@exprs)
           (finally ~@closes))))))

(defn query
  "Executes a query that returns some results (e.g. select)
   args: [conn sql bind-params-vec]
     params should be a vector of param values
   returns: resultset or nil"
  [conn sql bind-params-vec]
  (j/query conn (into [sql] bind-params-vec)))

(defn execute
  "Executes a query that does not return a result with zero or more sets of params
   args: [conn sql & bind-params-vecs]
   returns: vector of update counts"
  [conn q & bind-params-vecs]
  (j/execute! conn (into [q] bind-params-vecs)
              {:transaction? false  :multi? true}))

(defn q [pool sql args]
  (with-conn [conn pool]
    (query conn sql args)))

(def isolations
  #{:none            
    :read-uncommitted ; prevent dirty write
    :read-committed   ; prevent dirty read
    :repeatable-read  ; prevent fuzzy read
    :serializable})   ; prevent phantoms

(defn transact
  "Executes a given function within a transaction
   args: [conn isolation read-only? func]
     conn: a connection as obtained through with-conn
     isolation: one of :none, :read-uncommitted, :read-committed,
                       :repeatable-read, :serializable
     read-only?: boolean"
 [conn isolation read-only? func]
  (when-not (isolations isolation)
    (throw (ex-info (str "transact: invalid isolation: " isolation)
                    {:got isolation  :valid isolations})))
  (j/db-transaction* conn func {:isolation isolation  :read-only? read-only?}))

(defmacro in-transaction
  "[macro] Executes code within a transaction
   args: [conn isolation roness & exprs]
     conn: a connection as obtained through with-conn
     isolation: one of :none, :read-uncommitted, :read-committed,
                       :repeatable-read, :serializable
     writability: one of :ro, :rw. Whether to lock for read only"
  [conn isolation roness & exprs]
  `(transact ~conn ~isolation ~(case roness :ro true :rw false)
     (fn [~'_] ~@exprs)))
