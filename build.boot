; vim: syntax=clojure ts=2 sw=2 et
(set-env!
 :project 'irresponsible/utrecht
  :version "0.2.0"
  :resource-paths #{"src"}
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure       "1.8.0" :scope "provided"]
                  [org.clojure/java.jdbc     "0.6.0-alpha2"]
                  [hikari-cp                 "1.6.1"]
                  ;; don't force the user to pull in any of these
                  [com.stuartsierra/component "0.3.1"   :scope "test"]
                  [suspendable "0.1.1"                  :scope "test"]
                  [org.postgresql/postgresql "9.4.1208" :scope "test"]
                  [environ "1.0.2"                      :scope "test"]
                  [adzerk/boot-test          "1.1.0"    :scope "test"]])
                  
(require '[adzerk.boot-test :as t])

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)
       :description "Just enough rope to wrangle a jdbc"
       :url "https://github.com/irresponsible/utrecht"
       :scm {:url "https://github.com/irresponsible/utrecht.git"}
       :license {"MIT" "https://en.wikipedia.org/MIT_License"}}
  target  {:dir #{"target"}})

(deftask testing []
  (set-env! :source-paths   #(conj % "test")
            :resource-paths #(conj % "test"))
  identity)

(deftask test []
  (comp (testing) (t/test)))

(deftask autotest []
  (comp (testing) (watch) (t/test)))

(deftask make-release-jar []
  (comp (pom) (jar)))

(deftask travis []
  (testing)
  (t/test))

(deftask installdeps []
  identity)
