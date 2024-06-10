(ns botsad.crawler
  (:require [clojure.string :as str]))

(def ids (str/split (slurp "db/ids.txt") #"\n"))

(doseq [id ids]
   (println id)
   (spit (str "site/" id ".html")
         (slurp (str "https://tools.bgci.org/garden.php?id=" id)))
   (Thread/sleep 2000))

