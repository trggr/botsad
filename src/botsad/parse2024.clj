;; From https://gist.github.com/borkdude/fc64444a4e7aea4eb647ce42888d1adf
(ns parse2024
  (:require [babashka.pods :as pods]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [pod.retrogradeorbit.bootleg.utils :as bootleg]
            [babashka.curl :as curl]
            [pod.retrogradeorbit.hickory.select :as s]))

;; only need it once
;; (pods/load-pod 'retrogradeorbit/bootleg "0.1.9")
(pods/load-pod "bootleg") ;; installed on path, use "./bootleg" for local binary

;; (def irkutsk-html (slurp "db/112.html"))

(def irkutsk-html (-> "file:///c:/apps/0atl/botsad/db/112.html"
                      curl/get
                      :body))

irkutsk-html

;; (def hiccup (bootleg/convert-to irkutsk-html :hiccup))
(def hiccup (bootleg/html->hiccup-seq irkutsk-html))
hiccup

(def hickory (bootleg/html->hickory-seq irkutsk-html))
hickory


(def divs (atom []))

(def x (walk/postwalk
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
        hiccup))

(count @divs)

;; this works
;; (map (fn [[_div _class _space h3 & more]] h3) @divs)

;; (def fields (atom []))

;; (defn find-ul [node]
;;   (let [fields []]
;;     (when (string? node)
;;       (let [s (str/trim node)]
;;         (when (pos? (count s))
;;           (conj fields s))))
;;     fields))

;; (defn find-ul2 [node]
;;   (if (string? node)
;;     (str/trim node)
;;     nil))

;; (map (fn [[_div _class _space _h3 & more]]
;;        (walk/postwalk find-ul2 more))
;;      (take 2 @divs))

(map (fn [[_div _class _space _h3 & more]]
       (reduce (fn [acc s]
                 (if (not (string? s))
                   acc
                   (let [t (str/trim s)]
                     (if (pos? (count t))
                       (conj acc t)
                       acc))))
               []
               (flatten more)))
     (take 5 @divs))

(let [[_div _class _space _h3 & more] (first @divs)]
  (flatten more))

@divs
@fields

(nth @divs 3)

@tables
(last @tables)

(nth @tables 10)

(second @tables)

(some #{1 2 3} [1 2])

(def tables-hickory (s/select (s/tag :table) hickory))

(s/select (s/tag :article) hickory)

(s/tag :article)

(get #{"meta-heading first"
        "meta-heading"
        "meta-heading last"} "meta-heading first")

(spit "/tmp/00.txt" @tables)