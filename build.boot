; vim: syntax=clojure ts=2 sw=2 et
(set-env!
 :project 'irresponsible/utrecht
  :version "0.2.1"
  :resource-paths #{"src"}
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure       "1.9.0-alpha15" :scope "provided"]
                  [org.clojure/java.jdbc     "0.7.0-alpha2"]
                  [hikari-cp                 "1.7.2"]
                  ;; don't force the user to pull in any of these
                  [com.stuartsierra/component "0.3.2"   :scope "test"]
                  [suspendable "0.1.1"                  :scope "test"]
                  [irresponsible/codependence "0.1.0"   :scope "test"]
                  [org.postgresql/postgresql "42.0.0.jre7" :scope "test"]
                  [environ "1.0.2"                      :scope "test"]
                  [adzerk/boot-test          "1.2.0"    :scope "test"]])
                  
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

(deftask installdeps []
  identity)

;; Release Manager
(deftask travis-installdeps []
  identity)

(deftask travis []
  (testing)
  (t/test))

(deftask jitpak-deploy []
  (task-options! pom {
    :project (symbol (System/getenv "ARTIFACT"))
  })
  (comp
    (pom)
    (jar)
    (target)      ; Must install to build dir
    (install)     ; And to .m2 https://jitpack.io/docs/BUILDING/#build-customization
  ))
