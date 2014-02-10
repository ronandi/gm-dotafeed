(ns gm-dotafeed.groupme-api
  (:require [clj-http.client :as http]))

(def ^:dynamic *token*)
(def api-root "https://api.groupme.com/v3")

(defn send-message
  [text & {:keys [token] :or {token *token*}}]
  (let [message-endpoint (str api-root "/bots/post")]
    (get-in (http/post message-endpoint {:form-params {:bot_id token :text text}})[:status])))
