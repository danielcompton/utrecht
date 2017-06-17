; CONFIGURATION
;
; This test needs UTRECHT_TEST_DB set to a disposable postgresql database
; to perform queries against.
;
; UTRECHT_TEST_HOST can be set to a server address (default: 127.0.0.1)
; UTRECHT_TEST_CONN_TIMEOUT can be set to the login wait time (default: 5000 (in milliseconds))
; UTRECHT_TEST_USER can be set to a username (default: empty)
; UTRECHT_TEST_PASS can be set to a password (default: empty)
; UTRECHT_TEST_PORT can be set to a port (default: 5432)

(ns irresponsible.utrecht-test
  (:use [clojure.test])
  (:require [irresponsible.utrecht :as u]
            [irresponsible.utrecht.component :as c]
            [irresponsible.utrecht.pool.hikaricp :refer [hikaricp]]
            [com.stuartsierra.component :refer [start stop]]
            [environ.core :refer [env]])
  (:import [java.sql PreparedStatement Savepoint]
           [clojure.lang ExceptionInfo]))


(def defaults
  {:postgres
   {:adapter "postgresql"
    :server-name "127.0.0.1"
    :port-number 5432
    :connection-timeout 5000}
   :pgjdbc-ng
   {:adapter "pgjdbc-ng"
    :host "127.0.0.1"
    :port 5432
    :connection-timeout 5000}})

(def config
  (let [uth  (env :utrecht-test-host)
        utct (env :utrecht-test-conn-timeout)
        utd  (env :utrecht-test-db)
        utu  (env :utrecht-test-user)
        utp  (env :utrecht-test-pass)
        utp2 (env :utrecht-test-port)]
    {:postgres
     (cond-> (:postgres defaults)
       uth  (assoc :server-name uth)
       utct (assoc :connection-timeout utct)
       utd  (assoc :database-name utd)
       utu  (assoc :username utu)
       utp  (assoc :password utp)
       utp2 (assoc :port-number (Integer/parseInt utp2)))
     :pgjdbc-ng
     (cond-> (:pgjdbc-ng defaults)
       uth  (assoc :host uth)
       utct (assoc :network-timeout utct)
       utd  (assoc :database utd)
       utu  (assoc :username utu)
       utp  (assoc :password utp)
       utp2 (assoc :port (Integer/parseInt utp2)))}))


(when-not (contains? (:postgres config) :database-name)
  (println "* Tests Skipped, UTRECHT_TEST_DB must be configured to a test Posgresql database"))

(deftest utrecht
  (if (contains? (:postgres config) :database-name)
    (doseq [k [:postgres :pgjdbc-ng]]
      (testing k
        (let [pool (hikaricp (config k))]
          (is (satisfies? u/Pool pool))
          (u/with-conn [c pool]
            (is (satisfies? u/Conn c))
            (is (satisfies? u/Conn c))
            (u/with-prep c [p "select 'foo' as result"]
              (is (instance? PreparedStatement p))
              (is (= '({:result "foo"})
                     (u/query c p)
                     (u/query c "select 'foo' as result")))
              (is (= [0] (u/execute c "create temporary table foo()")))))
          (try (u/with-transaction :ro :serializable [c pool]
                 (let [sp1 (u/savepoint c :sp1)]
                   (is (instance? Savepoint sp1))
                   (is (= '({:result "foo"})
                          (u/query c "select 'foo' as result")))
                   (is (nil? (u/rollback c sp1))))
                 (throw (ex-info "" {:error :yup})))
               (catch ExceptionInfo e
                 (is (= {:error :yup} (ex-data e)))))
          (.close pool))))
      (println "* Tests Skipped, UTRECHT_TEST_DB must be configured to a test Posgresql database")))
  
(deftest cpt
  (if (contains? (:postgres config) :database-name)
    (doseq [k [:postgres :pgjdbc-ng]]
      (testing k
        (let [pool (start (c/utrecht (config k)))]
          (is (satisfies? u/Pool pool))
          (u/with-conn [c pool]
            (is (satisfies? u/Conn c))
            (is (satisfies? u/Conn c))
            (u/with-prep c [p "select 'foo' as result"]
              (is (instance? PreparedStatement p))
              (is (= '({:result "foo"})
                     (u/query c p)
                     (u/query c "select 'foo' as result")))
              (is (= [0] (u/execute c "create temporary table foo()")))))
          (try (u/with-transaction :ro :serializable [c pool]
                 (let [sp1 (u/savepoint c :sp1)]
                   (is (instance? Savepoint sp1))
                   (is (= '({:result "foo"})
                          (u/query c "select 'foo' as result")))
                   (is (nil? (u/rollback c sp1))))
                 (throw (ex-info "" {:error :yup})))
               (catch ExceptionInfo e
                 (is (= {:error :yup} (ex-data e)))))
          (stop pool))))
      (println "* Tests Skipped, UTRECHT_TEST_DB must be configured to a test Posgresql database")))

