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
  [file-name]
  (with-open [in-file (io/reader file-name)] 
    (let [lines (take 10 (line-seq in-file))]
      (vec (rest (str/split (apply str (filter
                                        #(re-matches #"^#fields\t.*" %)
                                        lines))
                            #"\t"))))))

;;; todo - figure out log type based on header
(defn bro-process-log
  "read log and do stuff"
  [file-name]
  (with-open [in-file (io/reader file-name)]
    (->>
     (csv/parse-csv in-file
                  :delimiter \tab)
     remove-comments
     (mappify {:header (bro-fields file-name)})
     doall)))
  
(defn -main
  "do stuff"
  [& args]
  (println "Separator:" (bro-log-get-separator connlog))
  (println "Fields:" (bro-fields connlog))
  (println "Ten lines from connlog:")
  (println (take 10 connlog) "\n")

  (println "attempting (bro-process-log)...")
  (doall (map #(println %) (bro-process-log "conn.log")))
  ;; (clojure.pprint/print-table (bro-process-log "conn.log"))
  ;; (with-out-str (clojure.pprint/pprint (foo "conn.log")))
  ;; (doseq #(-> % str println) (foo** "conn.log"))

  )
