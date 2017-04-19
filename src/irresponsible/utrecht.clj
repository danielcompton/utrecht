(ns irresponsible.utrecht
  (:require [clojure.java.jdbc :as j])
  (:import  [java.sql Connection Savepoint]))

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
     throws: if we cannot obtain a connection")
  (transact [pool lock-mode isolation func]
   "Executes a given function within a transaction
    args: [pool lock-mode isolation lock-mode func]
      lock-mode: one of :ro, :rw. Whether to lock for read only
      isolation: one of :none, :read-uncommitted, :read-committed,
                        :repeatable-read, :serializable"))

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
  (extend-type Connection
    Conn
    (prepare [c sql]
      (io! (j/prepare-statement c sql)))
    (query
      ([^Connection c q]
       (io! (j/query {:connection c} [q])))
      ([^Connection c q bs]
       (io! (j/query {:connection c} (into [q] bs)))))
    (execute
      ([^Connection c q]
       (io! (j/execute! {:connection c} [q] {:transaction? false})))
      ([^Connection c q bs]
       (io! (j/execute! {:connection c} (into [q] bs) {:transaction? false}))))
    (savepoint [^Connection c name]
      (io! (.setSavepoint c (kw-str name))))
    (rollback [^Connection c sp]
      (io!
       (if (instance? Savepoint sp)
         (.rollback c ^Savepoint sp)
         (.rollback c ^String (kw-str sp)))))))

(defmacro with-conn
  "[macro] Executes code within the scope of a connection to the database
   args: [[name pool] & exprs]
     name: symbol to bind the connection to in the scope
     pool: pool as returned from make-pool
     writability: one of :ro, :rw. Whether to lock for read only"
  [[name pool] & exprs]
  `(io! (let [~name (get-conn ~pool)]
          (try ~@exprs
               (finally (.close ~name))))))

(defmacro with-prep
  "[macro] Executes exprs in the context of one or more queries which will be prepared and then closed after the scope closes
   args: [conn query-bindings & exprs]
     query-bindings: 'assignment vector' of symbol to sql string
                     each symbol will be bound to a PreparedStatement"
  [conn names & exprs]
  (when (not= 0 (mod (count names) 2))
    (throw (ex-info "with-prepared assignment vector is uneven" {})))
  (let [parts (partition 2 names)]
    `(io! (let ~(vec (mapcat (fn [[name sql]] [name `(prepare ~conn ~sql)]) parts))
            (try ~@exprs
                 (finally ~@(map (comp (partial list '.close) first) parts)))))))

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
