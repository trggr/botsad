How to Parse Botanical Gardens

At the bottom of "Advanced Search",

    https://tools.bgci.org/garden_advanced_search.php?action=Find&mode=&ftrCountry=All&ftrInstitutionType=All&ftrKeyword=&x=84&y=22#results

find and download a CSV file with a list of botanical gardens.
The file has approximately 3700 rows.
Each row contains an ID and the Name of every garden in the database. 

Once you know garden's ID, you can request a full information about it via:

        https://tools.bgci.org/garden.php?id=1024

I have made a simple crawler, which runs in a background, loops through a list of available IDs,
fetches an original page, and stores it in a file _site/1024.html_ .
To avoid overloading the site, the crawler sleeps 2 seconds before making a
next request.

     (def ids (split (slurp "db/ids.txt") #"\n"))

     (doseq [id ids]
        (println id)
        (spit (str "site/" id ".html")
              (slurp (str "https://tools.bgci.org/garden.php?id=" id)))
        (Thread/sleep 2000))

Once the files are retrieved and stored locally, we perform a series of steps
to extract the information. (Refer to the source code src/botsad/core.clj)

Step 1. Read a full content of a file into a string named P1
   
    p1  (slurp (str "site/" id ".html"))

Step 2. The information is between tags "article" and "/article".

    p2  (subs p1 (index-of p1 "<article>") (index-of p1 "</article>"))

Step 3. There are 10 blocks of information in each article. The blocks are separated by tags
"div class=". The variable P3 is an array of size 10. Each element is a string representing
on of the blocks. The first block is not important and we discard it. Now P4 is an array of only 9 elements.

    p3  (split p2 #"<div class=")
    p4  (rest p3)

The blocks contain the following:

     Block   Description   
     ------  ---------------
     0       not important
     1       Name
     2       Info
     3       Staff
     4       About
     5       Features
     6       Collections
     7       Conservation
     8       Research
     9       Education


Step 5. The most difficult block to parse is block Info (#2). 
Some gardens provide a very elaborate information about themselves, including pictures and 
specially formatted text. This is how we process this block:

    (let [p1 (subs art (index-of art "<div id=\"tabs-1\">") (index-of art "<div class=\"meta-heading first\">"))
          p2 (-> p1
                 (replace #"<a.*?>" "")     ; Replace everything within <a href=....> with empty string,
                                            ; effectively it removes all HTTP links
                 (replace #"<div.*?>" "")   ; remove <div ..> links
                 (replace #"<b>" "")        ; removes <b> - bold font tags
                 (replace #"</b>" "")       ; remove </b> - end of bold font tags
                 (replace #"</a>" "")       ;
                 (replace #"</p>" ";")      ; REPLACE <p> (end of paragraf) with semicolons. Explained later.
                 (replace #"<p>"  "")       ;  
                 (replace #"<p>" "")        ;
                 (replace #"<div>" "")      ;
                 (replace #"<h2>" "")       ;
                 (replace #"</h2>" "")      ;
                 (replace #"<br />" ";")    ; Another replace
                 (elim #"\s+" " "))         ; Replace all empty spaces - tabs, carriage, returns, spaces with a single space
           p3  (apply str p2)]
      p3))


Step 6. The rest of the blocks are similar to each other, and we apply the same logic to all of them. Function
ELIM works as replace, only it works on all blocks at once.

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

Step 7. I name the first block NAME, I ignore the second and third blocks, and I name the
remaining blocks OTHERS

            [name _ _ & others] p5

Step 8. At this point each block is a string:

    "Altitude:	0.00 Metres ; Institution Type: Botanic Garden; Latitude: 21.2848800;"

Step 9. We make a function TOMAP, which turns a block into key-value pairs.
The function takes a block's number - IDX, and a block's content - ITEM. 
It then splits ITEM into array at each semicolon (rememeber the semicolons from steps 5 and 6?), and further splits 
each element at a colon ':' into pairs.
In a pairs, the first element is called KEY, and the second element VALUE.
The KEYS are unique. We then leave only true pairs - filter where count = 2. For example, after TOMAP is applied to a 
second block, to a string above, we are getting:

    Key                 Value
    ---------           -------------
    2-Altitue           0.00 Metres
    2-Institution Type  Botanic Garden 
    2-Latitue           21.2848800

the source of TOMAP:

    (defn tomap [idx item]
       (->> (split item #";")
            (map #(split % #":"))
            (filter #(= (count %) 2))
            (reduce (fn [acc [k v]] (assoc acc (str (+ 2 idx) "-" (trim k)) (trim v))) {})))

Step 10. At this point we have a list of maps, which we then merge into a single map M:

     m  (reduce merge (sorted-map) (map-indexed tomap others))

Step 11. The section INFO had a special treatment on step 5. We merge it to M separately:

     m  (merge m (tomap 7 info))

Step 12. We are adding ID, garden's name, and info to the map M, and call the result P6:

     p6 (assoc m "0-ID" id, "1-Name" name, "2-Info" info)

Step 13. At this point we have our garden looks like a map between a key and a value:

     [0-ID:                1024, 
      1-Name:              Myall Park Botanic Garden
      2-Altitute:          0.00 Meters
      2-Institution Type:  Botanic Garden 
      2-Latitue:           21.2848800
      4-Info:              text
      . . .
      . . .]

We turn a map into a string string P7:

      p7 (join \newline (for [[k v] p6] (format "%s: %s" k v)))
      (spit (str "parsed/" id ".txt") p7)

Step 14. And save it to a file _parsed/[garden-ID].txt_

      (spit (str "parsed/" id ".txt") p7)

Step 15. We take the next ID, and repeat the process starting from step 1, until all IDs are processed.
We now have a two-dimensional array where rows correspond to each garden, and columns represent a piece of information:
ID, Name, Latitude, etc.

Step 16. Due to difference in formatting of each garden, we are have about 700 keys of information.
Some of them are junk, and before exporting the information to Excel, I calculated the frequencies of each
key, and took the first 100 most used or "good" keys.
The good key means it's shorter than 50 characters and it's among top 100 most frequent keys:

    (def allkeys (filter #(< (count %) 50) (flatten (map keys db))))
    (spit "good-keys.txt" (join \newline (sort (map first (take 100 (sort-by val > (frequencies allkeys)))))))


Step 17. This step saves the info into Excel

    (defn export-xls [xls file]
        (let [w      (create-workbook "Gardens" xls)
              s      (select-sheet "Gardens" w)]
          (save-workbook! file w)))

    (def header (map #(subs % 2) good-keys))
    (def rows   (map (fn [garden] (map #(get garden %) good-keys)) db))
    (def xls (into [header] rows))
    (export-xls xls "gardens.xlsx")

Clojure is an excellent language, but not an easy language to learn.
I used it on and off for close to 10 years for hobby and professional projects, but can't
say I am an expert in it. The code is very terse (core.clj + crawler.cly together are 139 lines),
but efficient and runs fast. 



