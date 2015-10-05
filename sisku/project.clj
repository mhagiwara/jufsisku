(defproject sisku "1.0.0-SNAPSHOT"
  :description "lojbo xelfanva sisku - Lojban translation search"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "1.0.0"]
                 [congomongo "0.1.8"]
		 [lein-ring "0.4.6"]]
  :dev-dependencies [[lein-ring "0.4.6"]]
  :main misc.sisku.core)