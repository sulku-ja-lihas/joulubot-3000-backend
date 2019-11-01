(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [ring.middleware.json :refer [wrap-json-response]]
            [clojure.data.json :as json]
            [compojure.handler :refer [site]]
            [ring.util.response :refer [response]]
            [compojure.route :as route]
            [monger.collection :as mc]
            [monger.core :as mg]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [clj-http.client :as client]
            [clojure.data.json :as json]))
(use '[ring.middleware.json :only [wrap-json-body]])

(defn send-to-slack [text]
  (client/post (env :write-hook)
    {:form-params {:payload (json/write-str {:text text})}}))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Here cometh thy challenge... sent"})

(defn make-response [data]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body data})

(def connection-url
  (env :mombodb))

(defn write-to-db [msg]
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/insert-and-return db "test" {:msg msg})))

(defn read-random-stuff []
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/find-maps db "test" {})))

(def tila (atom {}))

(defroutes main-routes
  (GET "/" []
       (send-to-slack "HELLOBOYS")
       (splash))
  (GET "/ping" []
       (assoc (splash) :body "Ping ping vaan itelles"))
  (POST "/challenge" req
        (assoc (splash) :body (get-in (req :body) [:challenge])))
  (POST "/save" req
        (comment "joo tässä voi sitten tallentaa tietokantaan vaikka (write-to-db data) kunhan sen payloadin saa jotenkin kaivettua tosta perkeleen reqista. Ei kiinnosta ja fuck the world"))
  (GET "/db" []
       (make-response (read-random-stuff)))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(def app
  (-> main-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn -main [& [port]]
(let [port (Integer. (or port (env :port) 5000))]
  (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
