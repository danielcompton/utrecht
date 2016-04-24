(ns irresponsible.utrecht.component
  (:require [com.stuartsierra.component :refer [Lifecycle start stop]]
            [suspendable.core :refer [Suspendable]]))

(defn destate [c]
  (reduce dissoc c (:-state c)))

(defrecord Utrecht
    [-server -starter]
  Lifecycle
  (start [self]
    (if -server  self
      (->> (-starter (destate self))
           (assoc-in self :-server))))
  (stop [self]
    (when -server
      (.stop -server))
    (dissoc self :server))
  Suspendable
  (suspend [self] self)
  (resume [self old]
    (if (= (destate self) (destate old))
      (assoc self :-server (:-server old))
      (do (stop old)
          (start self)))))

(defn utrecht
  [conf]
  (->> {:-state [:-server :-starter]}
       (merge conf)
       map->Utrecht))
