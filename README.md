On Advanced Search page, at the bottom, there's a link to CSV file, which you can download.

    https://tools.bgci.org/garden_advanced_search.php?action=Find&mode=&ftrCountry=All&ftrInstitutionType=All&ftrKeyword=&x=84&y=22#results
    https://tools.bgci.org/garden_advanced_search.php?action=Find&mode=&ftrCountry=All&ftrInstitutionType=All&ftrKeyword=&x=84&y=22&export=1

The file contains a list of pairs - ID and the Name of every garden in the database. There are total of 3693 pairs in the file.

You can request a full information about a garden by providing its ID via a request:

        https://tools.bgci.org/garden.php?id=1024

I have made a simple crawler, which runs in a background, loops through a list of IDs, fetches an original page,
and stores it in a file

    site/1024.html

The crawler sleeps 2 seconds, and repeats a process for the next ID.

The crawlers's source code is below:

     (def ids (drop 10 (split (slurp "db/ids.txt") #"\n")))

     (doseq [id ids]
        (println id)
        (spit (str "site/" id ".html")
              (slurp (str "https://tools.bgci.org/garden.php?id=" id)))
        (Thread/sleep 2000))

Once the crawler finishes, I have a 3693 HTML pages stored locally.

The next step is to understand how each page is organized. The page has a lot in it, but the
most interesting part is between tags "article" and "/article".

I loop through HTML files one by one, and perform a series of steps on each file.
(Refer to the source file src/botsad/core.clj)

Step 1. Read a full content of a file into a string named P1
   
    p1  (slurp (str "site/" id ".html"))

Step 2. Get a part of the string between tags <article> and </article>

    p2  (subs p1 (index-of p1 "<article>") (index-of p1 "</article>"))

Step 3. There are 10 blocks in each article. The blocks are separated by tags "div class=". I split
the document into blocks. In here, the variable P3 is a array of strings, where each string represents one of
the blocks:

    p3  (split p2 #"<div class=")

Step 4. Each blocks represent a group of information:

     1 - no important information
     2 - Name
     3 - Info
     4 - Staff
     5 - About
     6 - Features
     7 - Collections
     8 - Conservation
     9 - Research
     10 - Education

so I get rid of the first block:

     p4  (rest p3)

Step 5. The most difficult block is Info. Some gardens provide very little info about themselves, and they
are easy to process. The others have a very elaborate information, including pictures.
This is how I process this block:

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


Step 6. All following blocks in article are very similar to each other. There's a signle logic for all of them. Function
ELIM works as replace, only it loops through each block.

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

Step 8. At this point each block looks like this:

    "Altitude:	0.00 Metres  ; Institution Type:	Botanic Garden; Latitude:	21.2848800;"

Step 9. We write a function TOMAP, which takes a block and turns into a list of key-value pairs.
This function takes a block number IDX, and a block's content named ITEM. 
It then splits ITEM into array at each semicolon (rememeber the semicolons from steps 5 and 6?), and further splits 
each element at a colon ':' into pairs. In a pairs, the first element is called KEY, and the second element VALUE.
The KEYS are unique. We then leave only true pairs - filter where count = 2. For example, after TOMAP is applied to a 
second block, to a string above, we are getting:

    Key                 Value
    ---------           -------------
    2-Altitue           0.00 Metres
    2-Institution Type  Botanic Garden 
    2-Latitue           21.2848800

the source of TOMAP is very terse:

    (defn tomap [idx item]
       (->> (split item #";")
            (map #(split % #":"))
            (filter #(= (count %) 2))
            (reduce (fn [acc [k v]] (assoc acc (str (+ 2 idx) "-" (trim k)) (trim v))) {})))

Step 10. We take all maps, and merge them into one map M:

     m  (reduce merge (sorted-map) (map-indexed tomap others))

Step 11. The section INFO had a special treatment on step 5. We merge it to M separately:

     m  (merge m (tomap 7 info))

Step 11. The section INFO had a special treatment on step 5. We merge it to M separately:

     m  (merge m (tomap 7 info))

Step 12. We are adding ID, garden's name, and info to the map M, and call the result P6:

     p6 (assoc m "0-ID" id, "1-Name" name, "2-Info" info)

Step 13. At this point we have an information about a single garden saved into array.

     [0-ID:                1024, 
      1-Name:              Myall Park Botanic Garden
      2-Altitute:          0.00 Meters
      2-Institution Type:  Botanic Garden 
      2-Latitue:           21.2848800
      4-Info:              text
      . . .
      . . .]

Step 14. This code turns a map P6, into a string P7:

      p7 (join \newline (for [[k v] p6] (format "%s: %s" k v)))
      (spit (str "parsed/" id ".txt") p7)

Step 15. We now print P7 to a file "parsed/1024.txt"

      (spit (str "parsed/" id ".txt") p7)

Step 16. After the loop through IDs is finished we now have a two-dimensional array
in which each row represent one garden, and each column represent one of the keys. For example,
all IDs are grouped together in a single column, all garden names, all latitues, and so on.

Step 17. We are ready to export it to Excel, but first, due to difference in formatting of each garden,
we realized that there are more than 700 keys. Which means the Excel will have 700 columns! I decided to take a
little research, and counted how many times each ID is presented for each garden. I then decided to take
top 100 keys, and discard the rest of them, assuming that they are unique to each garden, and that info
can better be obtained from the page of a garden directly. This peace of code, extract a list of good keys.
The good key means it's shorter than 50 characters and it's among top 100 most frequent keys:

    (def allkeys (filter #(< (count %) 50) (flatten (map keys db))))
    (spit "good-keys.txt" (join \newline (sort (map first (take 100 (sort-by val > (frequencies allkeys)))))))


Step 18. This step saves the info into Excel

    (defn export-xls [xls file]
        (let [w      (create-workbook "Gardens" xls)
              s      (select-sheet "Gardens" w)]
          (save-workbook! file w)))

    (def header (map #(subs % 2) good-keys))
    (def rows   (map (fn [garden] (map #(get garden %) good-keys)) db))
    (def xls (into [header] rows))
    (export-xls xls "gardens.xlsx")

The process is very briefly describes. Clojure is an excellent language, but it's not an easy language
by any means, I used it on and off for close to 10 years for hobby and professional projects, but can't
say I am an expert in it. The code is very terse (core.clj + crawler.cly together are 139 lines),
but efficient and runs fast. 



