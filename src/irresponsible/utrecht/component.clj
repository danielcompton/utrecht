(ns irresponsible.utrecht.component
  (:require [irresponsible.utrecht :as u]
            [irresponsible.utrecht.pool.hikaricp :refer [hikaricp]]
            [com.stuartsierra.component :refer [Lifecycle start stop]]
            [suspendable.core :refer [Suspendable]]))

(defrecord Utrecht
  [-pool]
  Lifecycle
  (start [self]
    (if -pool
      self
      (->> (hikaricp (dissoc self :-pool))
           (assoc self :-pool))))
  (stop [self]
    (when -pool (.close -pool))
    (dissoc self :-pool))
  Suspendable
  (suspend [self] self)
  (resume [self old]
    (if (= (dissoc self :-pool) (dissoc old :-pool))
      (assoc self :-pool (:-pool old))
      (do (stop old)
          (start self))))
  u/Pool
  (get-conn [self]
    (u/get-conn -pool))
  (transact [self lock-mode isolation func]
    (u/transact -pool lock-mode isolation func)))

(def utrecht map->Utrecht)
