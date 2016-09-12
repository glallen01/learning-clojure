(ns stuff.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [semantic-csv.core :as sc :refer :all]
   [clojure.string :as str]
   [clojure.pprint]
   [clojure-csv.core :as csv])
  (:import [java.util.zip GZIPInputStream])
  )

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
  "returns a vector of fields from the brolog header (can read .gz if named)"
  [file-name]
  (with-open [in-file
              (if (re-find #"\.gz$" file-name)
                (io/reader (GZIPInputStream. (io/input-stream file-name)))
                (io/reader file-name))
              ]
    (let [lines (take 10 (line-seq in-file))]
      (vec (rest (str/split (apply str (filter
                                        #(re-matches #"^#fields\t.*" %)
                                        lines))
                            #"\t"))))))

;;; todo - figure out log type based on header
(defn bro-process-log
  "read log and do stuff"
  [file-name]
  (with-open [in-file
              (if (re-find #"\.gz$" file-name)
                (io/reader (GZIPInputStream. (io/input-stream file-name)))
                (io/reader file-name))
              ]
    (->>
     (csv/parse-csv in-file
                  :delimiter \tab)
     remove-comments
     (mappify {:header (bro-fields file-name)})
     doall)))

(defn foo
  "count IPs"
  [brolog]
  (doall (map
          #(println (str %2 ": " (%1 :uid) "\t" (%1 :id.orig_h)))
          (bro-process-log "conn.log") (range))))

(defn foo*
  "count IPs"
  [brolog]
  (doall (map
          #(println (str %2 ": " (%1 :uid) "\t" (%1 :id.orig_h)))
          (bro-process-log "conn.log") (range))))

(defn bro-cut
  "print src dst port proto"
  [brolog & key-list]
  (map #(select-keys %
                             (if key-list
                               key-list
                               [:id.orig_h :id.orig_p :id.resp_h :id.resp_p]))
       brolog))

(defn -main
  "do stuff"
  [& args]
  (let [ *file-name* "conn.log"]
    (println "Separator:" (bro-log-get-separator connlog))
    (println "Fields:" (bro-fields *file-name*))
    (println "Ten lines from connlog:")
    (println (take 10 connlog) "\n")
    
    (println "attempting (bro-process-log)...")
    (doall (map #(println %) (bro-process-log *file-name*)))
    ;; (clojure.pprint/print-table (bro-process-log "conn.log"))
    ;; (with-out-str (clojure.pprint/pprint (foo "conn.log")))
    ;; (doseq #(-> % str println) (foo** "conn.log"))
    ))

;; (def foo (bro-process-log "conn.log"))
;; (apply map println foo)
