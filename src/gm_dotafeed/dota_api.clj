(ns gm-dotafeed.dota-api
  (:require [clj-http.client :as http]))

(def ^:dynamic *token*)
;Steam WebAPI Endpoints
(def ^:private hero-endpoint
  "http://api.steampowered.com/IEconDOTA2_570/GetHeroes/v1")

(def ^:private match-history-endpoint
  "http://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/v1")

(def ^:private match-details-endpoint
  "http://api.steampowered.com/IDOTA2Match_570/GetMatchDetails/v1")

;WebAPI Methods
(defn get-heroes
  [& {:keys [token] :or {token *token*}}]
    (get-in (http/get hero-endpoint {:query-params {:key token :language "en"} :as :auto}) [:body :result :heroes]))

(defn get-match-history
  [& {:keys [token] :or {token *token*} :as options}]
    (get-in (http/get match-history-endpoint {:query-params (assoc options :key token ) :as :auto}) [:body :result :matches]))

(defn get-match-details
  [match-id & {:keys [token] :or {token *token*} :as options}]
  (let [options (assoc options :key token :match_id match-id)]
    (get-in (http/get match-details-endpoint {:query-params options :as :auto}) [:body :result])))
