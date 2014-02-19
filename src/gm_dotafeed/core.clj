(ns gm-dotafeed.core
  (:require [gm-dotafeed.dota-api :as dota]
            [gm-dotafeed.groupme-api :as groupme]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as sql]))

(def api-key (env :steam-api-key))
(def gm-bot-key (env :dotafeed-bot-key))
(def db (or (System/getenv "DATABASE_URL") "postgresql://localhost:5432/dotafeed"))

(def friends {17782411 "Rohit" 88721757 "Tanay"
              115027706 "Daven" 33140492 "Rob"
              29991728 "Xavier" 8244147 "Apurva"})

(def relevant-modes #{1 2 3 4 5 12})

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

(defn pretty-player
  [heroes {:keys [hero_id account_id]}]
  (let [hero-name (get heroes hero_id)
        player-name (get friends account_id)]
    (str hero-name (when player-name (format "(%s)" player-name)))))

(defn pretty-print-match
  "Print out some match details"
  [match]
  (let [players (map #(select-keys % [:account_id :hero_id :player_slot]) (:players match))
        {radiant true dire false} (group-by #(> 5 (:player_slot %)) players)
        heroes (build-hero-map api-key)
        radiant (map (partial pretty-player heroes) radiant)
        dire (map (partial pretty-player heroes) dire)
        winner (if (:radiant_win match) "Radiant" "Dire")]
    (str "\nRadiant:\n" (apply str (interpose "\n" radiant)) "\n\nDire:\n" (apply str (interpose "\n" dire))
         "\n\n" winner " Victory.\ndotabuff.com/matches/" (:match_id match))))

(defn match-is-new
  [match-id]
  (empty? (sql/query db ["select * from matches where match_id = ?" match-id])))

(defn -main
  []
  (let [new-matches (filter match-is-new (last-matches (keys friends)))
        new-matches (map #(dota/get-match-details % :token api-key) new-matches)
        relevant-matches (filter (comp relevant-modes :game_mode) new-matches)]
    (doseq [match relevant-matches]
      (groupme/send-message (pretty-print-match match) :token gm-bot-key)
      (sql/insert! db :matches {:match_id (:match_id match)}))))
