(ns user
  (:use [clojure.string :as str]))

(defn tomap [idx item]
   (->> (split item #";")
        (map #(split % #":"))
        (filter #(= (count %) 2))
        (reduce (fn [acc [k v]] (assoc acc (str idx "-" (trim k)) (trim v))) {})))

(defn elim [s match replacement]
  (map #(str/replace % match replacement) s))

(def id 112)

(def p1 (slurp (str "site/" 112 ".html")))

; On the page, the information is between <article> and </article>
(def p2 (subs p1 (index-of p1 "<article>")
                 (index-of p1 "</article>")))

; each article has about 10 sections
(def p3 (split p2 #"<div class="))

; first section is useless
(def p4 (rest p3))

(def p5 (-> p4 
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
            (elim #"<a href=\"#tabs-1\">Garden Information</a>; <div id=\"tabs-1\"> <p>" "")
))

(def p6 (let [[name info & others] p5
              m (reduce merge (sorted-map) (map-indexed tomap others))]
           (assoc m "0-ID" id "1-Name" name "2-Info" info)))

(def p7 (join \newline 
           (for [[k v] p6]
               (format "%s: %s" (subs k 2) v))))

(println p7)


