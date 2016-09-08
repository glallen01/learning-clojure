(ns stuff.core
  (:gen-class))

(require 
 '[clojure.java.io :as io]
 '[semantic-csv.core :as sc :refer :all]
 '[clojure.string :as str]
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

(defn bro-fields [brolog]
  (rest (str/split (apply str (filter
                               #(re-matches #"^#fields\t.*" %)
                               brolog))
                   #"\t")))


(defn foo []
(with-open [in-file (io/reader "conn.log")]
  (->>
   (csv/parse-csv in-file
                  :delimiter \tab)
   remove-comments
   mappify
   doall)))

(defn main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Separator:" (bro-log-get-separator connlog))
  (println "Fields:" (bro-fields connlog))
  (println "Ten lines from connlog:")
  (println (take 10 connlog) "\n")
  (println "attempting foo...")
  (print foo)
  )
