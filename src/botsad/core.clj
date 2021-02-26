(ns botsad.core
  (:use [clojure.string :as str])
  (:use [dk.ative.docjure.spreadsheet]))

(defn tomap [idx item]
   (->> (split item #";")
        (map #(split % #":"))
        (filter #(= (count %) 2))
        (reduce (fn [acc [k v]] (assoc acc (str (+ 2 idx) "-" (trim k)) (trim v))) {})))

(defn elim [s match replacement]
  (map #(str/replace % match replacement) s))


(defn export-xls [xls file]
    (let [w      (create-workbook "Gardens" xls)
          s      (select-sheet "Gardens" w)
          header (first (row-seq s))]
      (set-row-style! header (create-cell-style! w {:background :gray, :font {:bold false}}))
      (save-workbook! file w)))

(def ids (map read-string (split (slurp "db/ids.txt") #"\n")))

(defn parse-info [art]
    (let [p1 (subs art (index-of art "<div id=\"tabs-1\">") (index-of art "<div class=\"meta-heading first\">"))
          p2 (-> p1
                 (replace #"<a.*?>" "")
                 (replace #"<div.*?>" "")
                 (replace #"<b>" "")
                 (replace #"</b>" "")
                 (replace #"</a>" "")
                 (replace #"</p>" ";")
                 (replace #"<p>"  "")
                 (replace #"<p>" "")
                 (replace #"<div>" "")
                 (replace #"<h2>" "")
                 (replace #"</h2>" "")
                 (replace #"<br />" ";")
                 (elim #"\s+" " "))
           p3  (apply str p2)]
      p3))


(defn parse-all [ids]
    (for [id ids]
       (let [p1  (slurp (str "site/" id ".html"))
             p2  (subs p1 (index-of p1 "<article>") (index-of p1 "</article>"))
             info (parse-info p2)
             p3  (split p2 #"<div class=")   ; each article has about 10 sections
             p4  (rest p3)                   ; the first one is useless
             p5  (-> p4 
                   (elim #"\s+" " ")
                   (elim #"\"meta-heading\"> <h3 class=\"closed\">" "")
                   (elim #"\"meta-heading first\"> <h3 class=\"closed\">" "")
                   (elim #"\"meta-heading last\"> <h3 class=\"closed\">"  "")
                   (elim #"\"breadcrumb\">BGCI > Garden</div> <h1>"       "")
                   (elim #"<a href=\"#tabs-1\">Garden Information</a>; <div id=\"tabs-1\"> <p>" "")
                   (elim #"\"edit-actions\">(.*)alt=\"Edit this page\" /></a>" "")
                   (elim #"<a href=\"#tabs-1\">" "")
                   (elim #"<a href=\"#tabs-3\">"  "")
                   (elim #"<a href=\"/joinin/members/\">" ";")
                   (elim #"</h1>"       "")
                   (elim #"<h3>"        "")
                   (elim #"<b>"         "")
                   (elim #"</b>"        "")
                   (elim #"</h3>"       "")
                   (elim #"</div>"      "")
                   (elim #"<div>"       "")
                   (elim #"<br />"      ";")
                   (elim #"</li> </ul>" ";")
                   (elim #"</li> <li>"  ";")
                   (elim #"<ul> <li>"   ";")
                   (elim #"<div id=\"tabs\">" "")
                   (elim #"<div id=\"tabs\">" "")
                   (elim #"<p>" "")
                   (elim #"</p>" ""))
             [name _ _ & others] p5
             m  (reduce merge (sorted-map) (map-indexed tomap others))
             m  (merge m (tomap 7 info))
             p6 (assoc m "0-ID" id, "1-Name" name, "2-Info" info)
;             p7 (join \newline (for [[k v] p6] (format "%s: %s" (subs k 2) v)))]
             p7 (join \newline (for [[k v] p6] (format "%s: %s" k v)))]
                (spit (str "parsed/" id ".txt") p7)
                p6)))


(def db (parse-all ids))

(map count db)

(def allkeys (filter #(< (count %) 50) (flatten (map keys db))))
(spit "good-keys.txt" (join \newline (sort (map first (take 100 (sort-by val > (frequencies allkeys)))))))

(def good-keys (sort (map first (take 100 (sort-by val > (frequencies allkeys))))))

(for [k good-keys]

;(frequencies allkeys)
;(take 100 (sort-by val > (frequencies allkeys)))
;
;(take 100 (sort-by val > (frequencies allkeys)))
;
;(spit "good-keys.txt" (sort (map first (take 100 (sort-by val > (frequencies allkeys))))))
;
;
;(set (flatten (map keys db)))
;
;                (println (count db)
;                         (keys (first db)
;                         (vals (first db))))

(defn main [& args] 1)






