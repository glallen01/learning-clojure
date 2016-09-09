(ns stuff.core
  (:gen-class))

(require 
 '[clojure.java.io :as io]
 '[semantic-csv.core :as sc :refer :all]
 '[clojure.string :as str]
 '[clojure.pprint]
 '[clojure-csv.core :as csv])


(defn log-lines [file-name]
  (-> (slurp file-name)
      (str/split-lines)))

(def connlog (log-lines "conn.log"))

(def comment-line-regexp #"^#.*")

(defn is-comment-line? [line]
  (re-matches comment-line-regexp line))

(def comment-lines (filter is-comment-line? connlog))

(defn bro-log-get-separator
  "returns the separator from a brolog"
  [brolog]
  (last (str/split (apply str (filter
                               #(re-matches #"^#separator (.*)" %)
                               connlog))
                   #" ")))

(defn bro-log-get-separator*
  "returns the separator from a brolog"
  [brolog]
  (-> (str/split
       (->> brolog
            (filter #(re-matches #"^#separator (.*)" %))
            (apply str))
       #" ")
      last))

(defn bro-fields
  "returns a vector of fields from the brolog header"
  [brolog]
  (vec (rest (str/split (apply str (filter
                                    #(re-matches #"^#fields\t.*" %)
                                    brolog))
                        #"\t"))))

;;; todo - figure out log type based on header
(defn foo**
  "read log and do stuff"
  [file-name]
  (with-open [in-file (io/reader file-name)]
    (->>
     (csv/parse-csv in-file
                  :delimiter \tab)
     remove-comments
     (mappify {:header (bro-fields connlog)})
     doall)))


(defn foo 
  "read log and do stuff"
  [file-name]
  (with-open [in-file (io/reader file-name)]
    (->>
     (csv/parse-csv in-file
                  :delimiter \tab)
     remove-comments
     (mappify {:header ["ts" "uid" "id_orig_h" "id_orig_p" "id_resp_h" "id_resp_p" "proto" "service" "duration" "orig_bytes" "resp_bytes" "conn_state" "local_orig" "local_resp" "missed_bytes" "history" "orig_pkts" "orig_ip_bytes" "resp_pkts" "resp_ip_bytes" "tunnel_parents"]})
     doall)))



(defn foo*
  "read log and do more stuff"
  [file-name]
  (with-open [in-file (io/reader file-name)]
    (doall
     (parse-and-process in-file
                        :delimiter \tab
                        ))))
  
(defn -main
  "do stuff"
  [& args]
  (println "Separator:" (bro-log-get-separator connlog))
  (println "Fields:" (bro-fields connlog))
  (println "Ten lines from connlog:")
  (println (take 10 connlog) "\n")

  (println "attempting foo...")
  (doall (map #(println %) (foo "conn.log")))
;  (with-out-str (clojure.pprint/pprint (foo "conn.log")))
  (clojure.pprint/print-table (foo "conn.log"))
  
  (println "attempting foo*...")
  (doall (map #(println %) (foo* "conn.log")))

  (println "attempting foo**...")
  (doall (map #(println %) (foo** "conn.log")))
  ;; (with-out-str (clojure.pprint/pprint (foo "conn.log")))
;;  (doseq #(-> % str println) (foo** "conn.log"))



  )
