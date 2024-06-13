;; From https://gist.github.com/borkdude/fc64444a4e7aea4eb647ce42888d1adf
(require '[babashka.pods :as pods])
(pods/load-pod "bootleg") ;; installed on path, use "./bootleg" for local binary

(require '[clojure.walk :as walk]
         '[clojure.string :as str]
         '[pod.retrogradeorbit.bootleg.utils :as bootleg])


;; only need it once
;; (pods/load-pod 'retrogradeorbit/bootleg "0.1.9")

;; (def irkutsk-html (slurp "db/112.html"))
;; (def irkutsk-html (-> "file:///c:/apps/0atl/botsad/db/112.html"
;;                       curl/get
;;                       :body))

;; (def hiccup (bootleg/html->hiccup-seq irkutsk-html))
;; hiccup

;; (def divs (atom []))

(defn extract-divs
  [hiccup]
  (let [divs (atom [])]
    (walk/postwalk
     (fn [node]
       (when (and (vector? node)
                  (= :div (first node))
                  (map? (second node))
                  (contains? (second node) :class)
                  (get #{"meta-heading first"
                         "meta-heading"
                         "meta-heading last"} (get (second node) :class)))
         (swap! divs conj node))
       node)
     hiccup)
    @divs))

(defn process-div-section-vector
  [acc [current after]]
  (if (or (not (string? current)) (not (string? after)))
    acc
    (let [current (str/trim current)
          after   (str/trim after)]
      (cond (= "Special Collections:" current)  (conj acc (str current " " after))

            (and (pos? (count current))
                 (str/includes? current ":"))   (conj acc current)

            :else                               acc))))

(defn process-div-section [section]
  (let [[_div _class _space _h3 & more] section
        more (flatten more)
        more (remove (fn [x] (contains? #{:br :ul :li :b} x)) more)
        pairs (partition 2 1 more)]
    (reduce process-div-section-vector
            []
            pairs)))


(defn process [file]
  (let [irkutsk-html (slurp file)
        hiccup (bootleg/html->hiccup-seq irkutsk-html)
        divs (extract-divs hiccup)]
    (flatten (map process-div-section divs))))

(defn -main [& args]
  (let [file (first args)
        kvs (process file)]
    (println "Humble beginning")
    (doseq [k kvs]
      (println k))))

(defn usage []
  (println "Extract text from botanic garden's web page")
  (println "bb parse2024.clj html-file"))

(when (= *file* (System/getProperty "babashka.file"))
  (if (zero? (count *command-line-args*))
    (usage)
    (apply -main *command-line-args*)))
