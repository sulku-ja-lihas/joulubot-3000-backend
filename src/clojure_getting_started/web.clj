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

(defn startRaffle [list-of-users]
  (map (fn [user] (-> (assoc {} :name user)
                      (assoc :tickets (+ (rand-int 40) 80)))) list-of-users))

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

(defn persist-raffle! [thread-id raffle]
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/insert-and-return db "raffles" {:_id thread-id})))

(defn select-raffle [thread-id]
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/find-map-by-id db "raffles" thread-id)))

(defn write-to-db [msg]
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/insert-and-return db "test" {:msg msg})))

(defn read-random-stuff []
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/find-maps db "test" {})))

(def tila (atom {}))
(defn json-response []
  {:status 200
   :headers {"Content-Type" "application/json"}})

(def channel-name "CPP1NF1MY")
(def token "xoxp-15568607280-297377650019-805201176466-19db6459b044e62fab6a51c68ce99133")
(def members-endpoint "https://slack.com/api/channels.info?token=xoxp-15568607280-297377650019-805201176466-19db6459b044e62fab6a51c68ce99133&channel=CPP1NF1MY&pretty=1")

(defn members-request []
  (client/post members-endpoint))

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
  (GET "/startraffle" req
    (assoc (json-response) :body (:body (members-request))))
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
