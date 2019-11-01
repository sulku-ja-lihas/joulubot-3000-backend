(defproject clojure-getting-started "1.0.0-SNAPSHOT"
  :description "Demo Clojure web app"
  :url "http://clojure-getting-started.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [com.novemberain/monger "3.1.0"]
                 [ring/ring-jetty-adapter "1.7.1"]
<<<<<<< HEAD
                 [ring/ring-json "0.5.0"]
                 [environ "1.1.0"]
                 [clj-http "3.10.0"]
                 [org.clojure/data.json "0.2.6"]]
=======
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-json "0.5.0"]
                 [environ "1.1.0"]]
>>>>>>> Öhöhöhö
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "clojure-getting-started-standalone.jar"
  :profiles {:production {:env {:production true}}}
  :main clojure-getting-started.web)
