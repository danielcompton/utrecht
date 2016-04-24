(ns irresponsible.utrecht-test
  (:use [clojure.test])
  (:require [irresponsible.utrecht :as u]
            [irresponsible.utrecht.pool.hikaricp :refer [hikaricp]]
            [environ.core :refer [env]])
  (:import [java.sql PreparedStatement]
           [clojure.lang ExceptionInfo]))

(def default-config
  {:adapter "postgresql"
   :server-name "127.0.0.1"
   :port-number 5432
   :database-name "utrecht_test"
   :connection-timeout 5000})

(def config
  (let [uth  (env :utrecht-test-host)
        utct (env :utrecht-test-conn-timeout)
        utd  (env :utrecht-test-db)
        utu  (env :utrecht-test-user)
        utp  (env :utrecht-test-pass)
        utp2 (env :utrecht-test-port)]
    (cond-> default-config
      uth  (assoc :server-name uth)
      utct (assoc :connection-timeout utct)
      utd  (assoc :database-name utd)
      utu  (assoc :username utu)
      utp  (assoc :password utp)
      utp2 (assoc :port-number (Integer/parseInt utp2)))))

(deftest utrecht
  (let [pool (hikaricp config)]
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
           (is (= [0] (u/savepoint c :sp1)))
           (is (= '({:result "foo"})
             (u/query c "select 'foo' as result")))
           (is (= [0] (u/rollback c :sp1)))
           (throw (ex-info "" {:error :yup})))
         (catch ExceptionInfo e
           (is (= {:error :yup} (ex-data e)))))
    (.close pool)))
