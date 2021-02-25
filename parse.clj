(ns user
  (:use [clojure.string :as str]))


(def ID 55)

(def p1 (slurp ("site/" ID ".html")))

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

(defn tomap [item]
   (->> (split item #";")
        (map #(split % #":"))
        (remove #(not= (count %) 2))
        (reduce (fn [acc [k v]] (assoc acc (trim k) (trim v))) {})))

(def p6 (let [[name info & others] p5
              m (reduce merge (map tomap others))]
           (assoc m "Gargen Name" name "Info" info)))


(get p6 "Ecosystem Conservation")

(map #(get p6 %) attribs)

(def attribs
 [
 "Director's Name"
 "Curator's Name"
 "Plant Records Officer's Name"
 "Research Staff Number"

 "Educational Staff Number"
 
 "Ecosystem Conservation"
 "Conservation - Biology"
 "Longitude"
 "Seed/Spore Biology"
 "Date founded"
 "Administration Staff Number"
 "Guided Tours"
 "Landscaped Area"
 "Altitude"
 "Conservation Programme"
 "Horticultural Staff Number"
 "Special Exhibitions"
 "Open to public"
 "Locality"
 "Conservation - Genetics"
 "Info"
 "Special Collections"
 "Gargen Name"
 "Data Management Systems and Information Technology"
 "Courses for University/College Students"
 "Permanent Public Displays"
 "Computer Plant Record System"
 "Education Signs in Garden"
 "Seed Bank"
 "Visitor/Education Centre"
 "Ecology"
 "Total Area"
 "Latitude"
 "Ex Situ Conservation Programme"
 "Courses for School Children"
 "Invasive Species Biology and Control"
 "Cultivation Taxa Num"
 "Institution Type"
 "Disabled access"
 "Friends society"
 "Public Lectures/Talks"
 "Systematics and Taxonomy"
 "Molecular Genetics"
 "Accession Number"
 "Natural Vegetation Area"
 "Biotechnology"
 "Number of Volunteers"
 "Local Address"
 "Annual Rainfall"
 "Herbarium"
 "Number of Visitors"
 "Restoration Ecology"])
