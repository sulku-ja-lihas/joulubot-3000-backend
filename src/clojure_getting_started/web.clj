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

(def hook-url (env :hook))

(def token (env :otoken))

(def connection-url
  (env :monbodb))

(defn entry-to-seq [{:keys [name tickets]}]
(repeat tickets name))

(defn pick-winner [raffle]
(->> raffle
      (map entry-to-seq)
      flatten
      shuffle
      rand-nth))

(defn persist-raffle! [thread-id raffle]
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/update db "raffles" {:_id thread-id :raffle raffle} {:upsert true})))

(defn send-to-slack [text]
  (client/post hook-url
               {:form-params {:payload (json/write-str {:text text})}}))

(defn start-raffle [list-of-users]
  (let [entries (map (fn [user] (-> (assoc {} :name user)
                  (assoc :tickets (+ (rand-int 40) 80)))) list-of-users)]
    (do
      (persist-raffle! "jou" entries)
      (send-to-slack (pr-str entries)) entries)))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Here cometh thy challenge... sent"})

(defn make-response [data]
  {:status 200
   :headers {"Content-Type" "text/json"}
   :body data})

(defn select-raffle [thread-id]
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/find-map-by-id db "raffles" thread-id)))

(defn write-to-db [msg]
  (let [{:keys [conn db]} (mg/connect-via-uri connection-url)]
    (mc/insert-and-return db "test" {:msg msg})))

(defn json-response []
  {:status 200
   :headers {"Content-Type" "application/json"}})

(def channel-name "CPP1NF1MY")
(def members-endpoint (str "https://slack.com/api/channels.info?token=" token "&channel=CPP1NF1MY&pretty=1"))

(defn members-request []
  (client/post members-endpoint))

(def winner-endpoint (str "https://slack.com/api/users.info?token=" token "&user="))

(defn winner-request [userid]
  (-> (client/post (str winner-endpoint userid))
      :body))

(def history-endpoint
    (str "https://slack.com/api/channels.history?token=" token "&channel=CPP1NF1MY"))

(defn bot-threadid-request []
  (let [messages ((json/read-str (:body (client/post history-endpoint))) "messages")]
    ((some #(= (% "bot_id") "BQ1QR81PS")) "ts")))

(def replies-endpoint
  (str "https://slack.com/api/channels.replies?token=" token "&channel=CPP1NF1MY"))

(defn thread-messages-request [threadid]
  (client/post (str replies-endpoint "&thread_ts=" threadid)))

(defroutes main-routes
  (GET "/" []
       (assoc (splash) :body "OK"))
  (GET "/ping" []
       (assoc (splash) :body "Ping ping vaan itelles"))
  (POST "/challenge" req
        (assoc (splash) :body (get-in (req :body) [:challenge])))
  (GET "/startraffle" req
       (let [members (get-in (json/read-str
                              (:body (members-request))
                              :key-fn keyword) [:channel :members])
             raffle (start-raffle members)]
         (assoc (json-response) :body raffle)))
  (GET "/finishraffle" []
       (let [winner (-> (select-raffle "jou")
                        :raffle
                        pick-winner
                        winner-request)]
         (do
           (send-to-slack winner)
           (assoc (splash) :body winner))))
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
