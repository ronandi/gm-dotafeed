(ns gm-dotafeed.core
  (:require [gm-dotafeed.dota-api :as dota]
            [gm-dotafeed.groupme-api :as groupme]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as sql]))

(declare build-hero-map)
(declare initialize-seen-matches)
(def api-key (env :steam-api-key))
(def gm-bot-key (env :dotafeed-bot-key))
(def db (or (System/getenv "DATABASE_URL") "postgresql://localhost:5432/dotafeed"))

(def friends {17782411 "Rohit" 88721757 "Tanay"
              115027706 "Daven" 33140492 "Rob"
              29991728 "Xavier" 8244147 "Apurva"})

(defn build-hero-map
  [api-key]
  "Return a id -> name mapping"
  (let [raw-hero-data (dota/get-heroes :token api-key)
        num-heroes (count raw-hero-data)
        hero-map (vec (repeat num-heroes nil))]
    (reduce #(assoc % (:id %2) (:localized_name %2)) hero-map raw-hero-data)))

(defn last-matches
  "Return a set of ids of the last matches friends played"
  [friends-ids]
  (let [matches (map
                 #(dota/get-match-history :token api-key :matches_requested 1 :account_id %)
                 friends-ids)
        matches (flatten matches)]
    (set (map :match_id matches))))

(defn populate-names
  [heroes {:keys [hero_id account_id]}]
  {:hero (get heroes hero_id) :player-name (get friends account_id)})

(defn pretty-player
  [heroes {:keys [hero_id account_id]}]
  (let [hero-name (get heroes hero_id)
        player-name (get friends account_id)]
    (str hero-name (when player-name (format "(%s)" player-name)))))

(defn pretty-print-match
  "Print out some match details"
  [match]
  (let [players (map #(select-keys % [:account_id :hero_id :player_slot]) (:players match))
        {radiant true dire false} (group-by #(< 5 (:player_slot %)) players)
        heroes (build-hero-map api-key)
        radiant (map (partial pretty-player heroes) radiant)
        dire (map (partial pretty-player heroes) dire)
        winner (if (:radiant_win match) "Radiant" "Dire")]
    (str "Radiant: " (apply str (interpose ", " radiant)) " vs. Dire: " (apply str (interpose ", " dire))
         ". " winner " Victory. dotabuff.com/matches/" (:match_id match))))

(defn match-is-new
  [match-id]
  (empty? (sql/query db ["select * from matches where match_id = ?" match-id])))


(defn -main
  []
  (let [new-matches (filter match-is-new (last-matches (keys friends)))]
    (doseq [match-id new-matches]
      (groupme/send-message (pretty-print-match (dota/get-match-details match-id :token api-key)) :token gm-bot-key)
      (sql/insert! db :matches {:match_id match-id}))))


;For a list of friends, get each person's last played game. If we have not seen the game,
;display the results of the game

;Get last match for each person and make into a set (since people play together)
;For each unseen: get details of match. substitute player Ids for names of friends. Sub hero id for hero name

;Requirements - Mapping of friends to account ids, Mapping of Hero ids to hero names
