(ns botsad.core
  (:require [reaver                       :as reaver]
            [clojure.string               :as str]
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
  (let [[_all _phone phone _fax fax _url url _email email]
        (re-find #"(Telephone:)(.*)(Fax:)(.*)(URL:)(.*)(Primary Email:)(.*)" s)]
    [["Phone" (str/trim phone)]
     ["Fax" (str/trim fax)]
     ["URL" (str/trim url)]
     ["Email" (str/trim email)]]))

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

(defn work
  [urls]
  (doseq [url urls]
    (println "Processing" url)
    (let [[_all _slash basename _ext] (re-find #"(/|\\)?(\d+)(.html|htm)" url)
          parsed (-> url slurp (str/replace #"</?[bi]>" "")
                     reaver/parse)]

      (spit (str basename ".txt")
            (with-out-str
              (println "ID = " basename)
              (doseq [[k v] (mapcat (fn [f] (f parsed))
                                    [extract-name-field extract-familiar-fields extract-meta-heading-fields])]
                (println k "=" v)))))))

(defn -main
  [& args]
  (when (zero? (count args))
    (println "Usage: reaver html-files")
    (System/exit 1))

  (work args))

(export-xls "a.xlsx" [["A11" "B12"][10 20][30 40]])
