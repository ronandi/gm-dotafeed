(defproject gm-dotafeed "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [environ "0.4.0"]
                 [clj-http "0.7.8"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [postgresql/postgresql "8.4-702.jdbc4"]]
  :plugins [[lein-environ "0.4.0"]])
