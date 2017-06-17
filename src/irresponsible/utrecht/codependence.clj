(ns irresponsible.utrecht.codependence
  (:require [irresponsible.codependence :as c]
            [irresponsible.utrecht :as u]
            [irresponsible.utrecht.pool.hikaricp :refer [hikaricp]]))

(defmethod c/start-tag :utrecht/hikaricp [_ config]
  {:pool
   (-> config
       (dissoc :co/tag :co/load)
       hikaricp)})

(defmethod c/stop-tag :utrecht/hikaricp [_ h]
  (-> h :pool .close))
