(ns botsad.core
  (:use [clojure.string :as str])
  (:use [dk.ative.docjure.spreadsheet]))

(defn tomap [idx item]
   (->> (split item #";")
        (map #(split % #":"))
        (filter #(= (count %) 2))
        (reduce (fn [acc [k v]] (assoc acc (str idx "-" (trim k)) (trim v))) {})))

(defn elim [s match replacement]
  (map #(str/replace % match replacement) s))


;(def p7 (join \newline 
;           (for [[k v] p6]
;               (format "%s: %s" (subs k 2) v))))
;
;(println p7)


;; Create a spreadsheet and save it
;(let [wb (create-workbook "Price List"
;                          [["Name" "Price"]
;                           ["Foo Widget" 100]
;                           ["Bar Widget" 200]])
;      sheet (select-sheet "Price List" wb)
;      header-row (first (row-seq sheet))]
;  (set-row-style! header-row (create-cell-style! wb {:background :yellow,
;                                                     :font {:bold true}}))
;  (save-workbook! "spreadsheet2.xlsx" wb))

(defn export-xls [xls file]
    (let [w      (create-workbook "Gardens" xls)
          s      (select-sheet "Gardens" w)
          header (first (row-seq s))]
      (set-row-style! header (create-cell-style! w {:background :gray, :font {:bold false}}))
      (save-workbook! file w)))

(def ids (map read-string (split (slurp "db/ids.txt") #"\n")))

(defn parse-all [ids]
    (for [id ids]
       (let [p1 (slurp (str "site/" id ".html"))
             p2 (subs p1 (index-of p1 "<article>") (index-of p1 "</article>"))
             p3 (split p2 #"<div class=")   ; each article has about 10 sections
             p4 (rest p3)                   ; the first one is useless
             p5 (-> p4 
                  (elim #"\s+" " ")
                  (elim #"\"meta-heading\"> <h3 class=\"closed\">" "")
                  (elim #"\"meta-heading first\"> <h3 class=\"closed\">" "")
                  (elim #"\"meta-heading last\"> <h3 class=\"closed\">"  "")
                  (elim #"\"breadcrumb\">BGCI > Garden</div> <h1>"       "")
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
                  (elim #"\"edit-actions\">(.*)alt=\"Edit this page\" /></a>" "")
                  (elim #"<div id=\"tabs\">" "")
                  (elim #"<div id=\"tabs\">" "")
                  (elim #"<a href=\"#tabs-1\">Garden Information</a>; <div id=\"tabs-1\"> <p>" ""))
             [name info & others] p5
             m  (reduce merge (sorted-map) (map-indexed tomap others))]
                (assoc m "0-ID" id "1-Name" name "2-Info" info))))

; (parse-all ids)
(set (flatten (map keys db)))

;                (println (count db)
;                         (keys (first db)
;                         (vals (first db))))

(defn main [& args] 1)





