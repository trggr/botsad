(ns botsad.core
  (:require [reaver                       :as reaver]
            [clojure.string               :as str]
            [clojure.pprint               :as pp]
            [dk.ative.docjure.spreadsheet :as xls]))

(defn get-key-and-value
  [s]
  (when-let [pos (str/last-index-of s ":")]
    [(str/trim (subs s 0 pos))
     (str/trim (subs s (inc pos)))]))

(defn extract-meta-heading-fields
  [parsed]
  (->> (reaver/extract parsed [] ".meta-heading > ul > li" reaver/edn)
       (map #(get % :content))
       (map #(remove map? %))
       flatten
       (map str/trim)
       (remove #(zero? (count %)))
       (map get-key-and-value)
       (remove nil?)))

(defn get-contact-fields
  [s]
;;   (println "here")
;;   (println "s=" s)
;;   (println "there")
  (let [[_all _phone phone _fax fax _url url _email email]
        (re-find #"(Telephone:)(.*)(Fax:)(.*)(URL:)(.*)(Primary Email:)(.*)" s)]
    (if (nil? phone)
      []
      [["Phone" (str/trim phone)]
       ["Fax" (str/trim fax)]
       ["URL" (str/trim url)]
       ["Email" (str/trim email)]])))

(defn get-familiar-field
  [s]
  (cond (or (str/starts-with? s "Institution Code:")
            (str/starts-with? s "BGCI Member:")
            (str/starts-with? s "Main Address:")) [(get-key-and-value s)]
        (str/starts-with? s "Telephone:") (get-contact-fields s)
        :else [["Mission" s]]))

(defn extract-familiar-fields
  [parsed]
  (->> (reaver/select parsed "article div#tabs-1 p")
       (map reaver/text)
       (mapcat get-familiar-field)))

(defn extract-name-field
  [parsed]
  [["Name" (reaver/text (reaver/select parsed "article h1"))]])

(defn export-xls [file xls]
  (let [w  (xls/create-workbook "Gardens" xls)
        _  (xls/select-sheet "Gardens" w)]
    (xls/save-workbook! file w)))

#_(defn parse-to-files
  [urls]
  (doseq [url urls]
    (println "Processing" url)
    (let [[_all _slash basename _ext] (re-find #"(/|\\)?(\d+)(.html|htm)" url)
          parsed (-> url slurp (str/replace #"</?[bi]>" "")
                     reaver/parse)]

      (spit (str "parsed/" basename ".txt")
            (with-out-str
              (println "ID = " basename)
              (doseq [[k v] (mapcat (fn [f] (f parsed))
                                    [extract-name-field extract-familiar-fields extract-meta-heading-fields])]
                (println k "=" v)))))))

(defn work
  [urls]
  (let [db
        (reduce (fn [acc url]
                  (println url)
                  (let [[_all _slash garden-id _ext] (re-find #"(/|\\)?(\d+)(.html|htm)" url)
                        parsed (-> url
                                   slurp
                                   (str/replace #"</?[bi]>" "")
                                   reaver/parse)]
                    (assoc acc
                           garden-id
                           (reduce (fn [garden [k v]]
                                     (assoc garden k v))
                                   (sorted-map)
                                   (mapcat (fn [f]
                                             (f parsed)) [extract-name-field
                                                          extract-familiar-fields
                                                          extract-meta-heading-fields])))))
                (sorted-map)
                urls)]

    (spit
     "parsed/DB.edn"
     (with-out-str
       (pp/pprint db)))))


(defn -main
  [& args]
  (when (zero? (count args))
    (println "Usage: reaver html-files")
    (System/exit 1))

  (work args))

(comment
  (def edn (read-string (slurp "parsed/DB.edn")))

  (count (keys edn))

  (defn edn->xls [edn-file])

  (def test {1 {:aa 1, :ab 2},
             2 {:ba 2, :bb 3}})

  (map (fn [[k v]] v) test)

  (spit "parsed/KEYS"
        (->> edn
             (map (fn [[_k v]] (keys v)))
             flatten
             (reduce (fn [acc k] (update acc k #(inc (or % 1))))
                     (sorted-map))
             pp/pprint
             with-out-str))

  (spit "parsed/KEYSTOP100"
        (->> edn
             (map (fn [[_k v]] (keys v)))
             flatten
             (reduce (fn [acc k] (update acc k #(inc (or % 1))))
                     (sorted-map))
             (filter (fn [[_k v]] (> v 100)))
             pp/pprint
             with-out-str))

  (def selected-keys
    (->> edn
         (map (fn [[_k v]] (keys v)))
         flatten
         (reduce (fn [acc k] (update acc k #(inc (or % 1))))
                 (sorted-map))
         (filter (fn [[_k v]] (> v 100)))
         (map first)))

  selected-keys

  (def rows   (map (fn [[id garden]]
                     (cons id
                           (map #(get garden %) selected-keys)))
                   edn))
  (def selected-keys-with-id (cons "ID" selected-keys))

  (def xls (cons selected-keys-with-id rows))



  (second xls)

  (export-xls "a.xlsx" xls)

  )
