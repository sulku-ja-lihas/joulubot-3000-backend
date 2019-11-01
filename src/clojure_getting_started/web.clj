(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [clj-http.client :as client]
            [clojure.data.json :as json]))

(defn send-to-slack [text]
  (client/post (env :write-hook)
    {:form-params {:payload (json/write-str {:text text})}}))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Here cometh thy challenge... sent"})

(defroutes app
  (GET "/" []
       (send-to-slack "HELLOBOYS")
       (splash))
  (GET "/ping" []
       (assoc (splash) :body "Ping ping vaan itelles"))
  (GET "/challenge" [req]
    (assoc (splash) :body (get-in req [:body :challenge])))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
