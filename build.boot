; vim: syntax=clojure ts=2 sw=2 et
(set-env! :dependencies (cond
  (= "1.8.0" (System/getenv "BOOT_CLOJURE_VERSION"))
    '[[org.clojure/clojure "1.8.0" :scope "provided"]
      [clojure-future-spec "1.9.0-alpha17"]]
  :else
    '[[org.clojure/clojure "1.9.0-alpha17" :scope "provided"]]))

(set-env!
 :project 'irresponsible/utrecht
  :version "0.3.0"
  :resource-paths #{"src" "resources"}
  :source-paths #{"src"}
  :dependencies #(into % '[[org.clojure/java.jdbc     "0.7.0-beta2"]
                  [hikari-cp                 "1.7.6"]
                  ;; don't force the user to pull in any of these
                  [com.stuartsierra/component "0.3.2"   :scope "test"]
                  [suspendable "0.1.1"                  :scope "test"]
                  [irresponsible/codependence "0.2.0"   :scope "test"]
                  [org.postgresql/postgresql "42.1.1"   :scope "test"]
                  [com.impossibl.pgjdbc-ng/pgjdbc-ng "0.7.1" :scope "test"]
                  [environ "1.0.2"                      :scope "test"]
                  [adzerk/boot-test          "1.2.0"    :scope "test"]]))
                  
(require '[adzerk.boot-test :as t])

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)
       :description "Just enough rope to wrangle a jdbc"
       :url "https://github.com/irresponsible/utrecht"
       :scm {:url "https://github.com/irresponsible/utrecht.git"}
       :license {"MIT" "https://en.wikipedia.org/MIT_License"}}
  push {:tag true
        :ensure-branch "master"
        :ensure-release true
        :ensure-clean true
        :gpg-sign true
        :repo "clojars"}
  target  {:dir #{"target"}})

(deftask testing []
  (set-env! :source-paths   #(conj % "test")
            :resource-paths #(conj % "test"))
  identity)

(deftask test []
  (comp (testing) (t/test)))

(deftask autotest []
  (comp (testing) (watch) (t/test)))

;; Release Manager
(deftask release []
  (comp (pom) (jar) (push)))
