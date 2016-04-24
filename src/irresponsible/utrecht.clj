(ns irresponsible.utrecht
  (:require [clojure.java.jdbc :as j]))

(def isolations
  #{:none            
    :read-uncommitted ; prevent dirty write
    :read-committed   ; prevent dirty read
    :repeatable-read  ; prevent fuzzy read
    :serializable})   ; prevent phantoms

(defprotocol Pool
  (get-conn [p]
    "Gets a connection from the pool. Is assumed to be Closeable.
     args: [pool]
     returns: Conn
     throws: if we cannot obtain a connection"))

(defprotocol Conn
  (prepare [c q]
    "Prepares the given sql query
     args: [conn sql]
     returns: java.sql.PreparedStatement
     throws: on i/o error")
  (query [c q] [c q bs]
    "Runs the given query and returns a seq of result maps
     args [conn q] [conn q bs]
       q: a sql string or PreparedStatement
       bs: optional vector of binding parameters
     returns: seq of map")
  (execute [conn q] [conn q bs]
    "Executes a query that does not return a result with zero or more sets of params
     args: [conn q] [conn q bs]
       q: a sql string or PreparedStatement
       bs: optional vector of binding parameters
     returns: vector of update counts")
  (savepoint [conn name]
    "Creates a savepoint with the given name;
     args: [conn name]")
  (rollback [conn name]
    "Rolls back the given transaction, optionally to the named savepoint
     args: [conn] [conn name]"))

(letfn [(kw-str [v]
          (cond (string? v) v
                (keyword? v) (name v)))]
  (extend-type java.sql.Connection
    Conn
    (prepare [c sql]
      (j/prepare-statement c sql))
    (query
      ([c q]
       (j/query {:connection c} [q]))
      ([c q bs]
       (j/query {:connection c} (into [q] bs))))
    (execute
      ([c q]
       (j/execute! {:connection c} [q] {:transaction? false}))
      ([c q bs]
       (j/execute! {:connection c} (into [q] bs) {:transaction? false})))
    (savepoint [c name]
      (execute c (str "savepoint " (kw-str name))))
    (rollback [c name]
      (execute c (str "rollback to " (kw-str name))))))

(defmacro with-conn
  "[macro] Executes code within the scope of a connection to the database
   args: [[name pool] & exprs]
     name: symbol to bind the connection to in the scope
     pool: pool as returned from make-pool
     writability: one of :ro, :rw. Whether to lock for read only"
  [[name pool] & exprs]
  `(let [~name (get-conn ~pool)]
     (try ~@exprs
       (finally (.close ~name)))))

(defmacro with-prep
  "[macro] Executes exprs in the context of one or more queries which will be prepared and then closed after the scope closes
   args: [conn query-bindings & exprs]
     query-bindings: 'assignment vector' of symbol to sql string
                     each symbol will be bound to a PreparedStatement"
  [conn names & exprs]
  (when (not= 0 (mod (count names) 2))
    (throw (ex-info "with-prepared assignment vector is uneven" {})))
  (let [parts (partition 2 names)]
    `(let ~(vec (mapcat (fn [[name sql]] [name `(prepare ~conn ~sql)]) parts))
       (try ~@exprs
            (finally ~@(map (comp (partial list '.close) first) parts))))))

(defn transact
  "Executes a given function within a transaction
   args: [conn lock-mode isolation lock-mode func]
     conn: a connection as obtained through with-conn
     lock-mode: one of :ro, :rw. Whether to lock for read only
     isolation: one of :none, :read-uncommitted, :read-committed,
                       :repeatable-read, :serializable"
  [conn lock-mode isolation func]
  (let [read-only? (case lock-mode :ro true :rw false)]
    (when-not (isolations isolation)
      (-> (str "transact: invalid isolation: " isolation)
          (ex-info {:got isolation :valid isolations}) throw))
    (j/db-transaction* {:datasource conn} func {:isolation isolation  :read-only? read-only?})))

(defmacro with-transaction
  "[macro] Executes code within a transaction, which auto commits at end
           unless of scope unless an exception is thrown (which rolls
           back the transaction and rethrows the exception)
   args: [conn isolation lock-mode & exprs]
     conn: a connection as obtained through with-conn
     lock-mode: one of :ro, :rw. Whether to lock for read only
     isolation: one of :none, :read-uncommitted, :read-committed,
                       :repeatable-read, :serializable"
  [lock-mode isolation [name pool] & exprs]
  `(transact ~pool ~lock-mode ~isolation
     (fn [{:keys [~'connection]}]
       (let [~name ~'connection]
         ~@exprs))))

