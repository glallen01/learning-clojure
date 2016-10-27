(ns stuff.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [semantic-csv.core :as sc :refer :all]
   [clojure.string :as string]
   [clojure.pprint]
   ;;[clj-mmap :as mmap]
   [clojure-csv.core :as csv]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.tools.trace :as trace]
   )
  (:import (java.util.zip GZIPInputStream)
           (org.apache.commons.compress.compressors
            xz.XZCompressorInputStream
            bzip2.BZip2CompressorInputStream))
  )


;; -------------- functions to handle options and exit

(def cli-options
  [["-t" "--type TYPE" "Log Type"
    :default "dns"]
   ["-v" nil "Verbosity level"
    :id :verbosity :default 0 :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-p" "--path PATH" "Path to logs"
    :default "/nsm/bro/logs"]
   ["-T" "--test" "Run the test fuction"
    :id :test]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["process bro-logs"
        ""
        "Options:"
        options-summary
        ""]
       (string/join \newline)))

(defn error-msg [errors]
  (str " **error**  " (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

;; -------------- 

(defn log-lines [file-name]
  (-> (slurp file-name) ;; need to get rid of slurp / switch to lazy
      (string/split-lines)))

(def connlog (log-lines "conn.log"))

(def comment-line-regexp #"^#.*")

(defn is-comment-line? [line]
  (re-matches comment-line-regexp line))

(def comment-lines (filter is-comment-line? connlog))

(defn bro-log-get-separator
  "returns the separator from a brolog"
  [brolog]
  (last (string/split (apply str (filter
                               #(re-matches #"^#separator (.*)" %)
                               connlog))
                   #" ")))

(defn bro-log-get-separator*
  "returns the separator from a brolog"
  [brolog]
  (-> (string/split
       (->> brolog
            (filter #(re-matches #"^#separator (.*)" %))
            (apply str))
       #" ")
      last))

(defn bro-logtype
  [file-name]
  (with-open [in-file
              (if (re-find #"\.gz$" file-name)
                (io/reader (GZIPInputStream. (io/input-stream file-name)))
                (io/reader file-name))
              ]
    (let [lines (take 10 (line-seq in-file))]
      (first (rest
              (string/split (apply str (filter
                                     #(re-find #"^#path" %)
                                     lines))
                         #"\t"))))))

(defn get-file-handle* [file-name]
  (let [rdr
        (if (re-find #"\.gz$" file-name)
          (io/reader (GZIPInputStream. (io/input-stream file-name)))
          (io/reader file-name))
        ]
    rdr))

(defn get-file-handle** [file-name]
  (try
    (io/reader (GZIPInputStream. (io/input-stream file-name)))
    (catch Exception e (io/reader file-name))))

(defn get-file-handle [file-name]
  (if (instance? java.io.File file-name)
    (def ^:private filenamestr (str (.getPath file-name)))
    (def ^:private filenamestr file-name))
  (cond
    (re-find #"\.gz$" filenamestr) (try
                                     (io/reader(GZIPInputStream. (io/input-stream file-name)))
                                     (catch Exception e (clojure.pprint/pprint e)))
    (re-find #"\.xz$" filenamestr) (try
                                     (io/reader (XZCompressorInputStream. (io/input-stream file-name)))
                                     (catch Exception e (clojure.pprint/pprint e)))
    :else (io/reader file-name)))

(defn bro-apply-regex-to-lines
  "return vectors of parsed lines"
  [regex lines]
  (->
   (->> lines
        (filter #(re-matches regex %))
        (apply str))
   (string/split #"\t")
   (rest)
   (vec)))

(defn bro-get-logfile-meta
  [file-name]
  (let [rdr (get-file-handle file-name)
        lines (take 8 (line-seq rdr))]
    {:path (first (bro-apply-regex-to-lines #"^#path\t.*" lines))
     :open (first (bro-apply-regex-to-lines #"^#open\t.*" lines))
     :fields (bro-apply-regex-to-lines #"^#fields\t.*" lines)
     :types (bro-apply-regex-to-lines #"^#types\t.*" lines)}))

(defn bro-types
   [file-name]
   (let [rdr (get-file-handle file-name)
         lines (take 8 (line-seq rdr))]
     (->
      (->> lines
           (filter #(re-matches #"^#types\t.*" %))
           (apply str))
      (string/split #"\t")
      (rest)
      (vec))))

(defn bro-fields
  "returns a vector of fields from the brolog header (can read .gz if named)"
  ([file-name]
   (let [rdr (get-file-handle file-name)
         lines (take 8 (line-seq rdr))]
     (vec (rest (string/split (apply str (filter
                                          #(re-matches #"^#fields\t.*" %)
                                          lines))
                              #"\t"))))) 
  ([file-name _]
   (let [rdr file-name
         lines (take 10 (line-seq rdr))]
     (vec (rest (string/split (apply str (filter
                                        #(re-matches #"^#fields\t.*" %)
                                        lines))
                            #"\t"))))))

;;; todo - figure out log type based on header
(defn bro-process-log
  "read and do stuff"
  ([file-name] (let [rdr (get-file-handle file-name)]
                 (println ">>> using file-name:" file-name)
                 (->>
                  (csv/parse-csv rdr :delimiter \tab)
                  remove-comments
                  (mappify {:header (bro-fields file-name)}))))
  ([file-handle _] (let [rdr file-handle]
                     (print ">>> using file-handle:" file-handle)
                           (->>
                            (csv/parse-csv rdr :delimiter \tab)
                            remove-comments
                            (mappify {:header (bro-fields rdr 1)})))))

(defn bro-process-log*
  "read and do stuff"
  ([file-name]
   (let [rdr (get-file-handle file-name)]
     (->>
      (csv/parse-csv rdr :delimiter \tab)
      remove-comments
      (mappify {:header (:fields (bro-get-logfile-meta file-name))})))))

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

(defn walk
  ;; example:
  ;; (map #(println (.getPath %)) (walk "." #".*\.log(\.gz)?"))
  [dirpath & pattern]
  (doall (filter #(re-find
                   (if pattern
                     (re-pattern (first pattern)) ; argh! ref: http://stackoverflow.com/questions/8205209/why-argument-list-as-arrayseq
                     #".*\.log(\.gz)?") ; all logs, compressed or not
                   (.getName %))
                 (file-seq (io/file dirpath)))))

(defn gather-log-types-in-dir
  ;; TODO - needs to gather both log-types, fields, and maybe fieldtypes?
  [dir]
  (map bro-fields (.list (io/file dir))))

(defn bro-epoch-to-dateObj
  [epoch-ns]
  (let [[s m] (string/split epoch-ns #"\.")]
    (java.util.Date. (+ (* (Long. s) 1000) (Long. m)))))

  
(defn test-function
  [options]
  "get a list of files"

  ;; lets do this!
  (def files (walk (:path options) #"dns.*\.log(\.gz)?(\.xz)?$"))
;;  (def files (walk (:path options) #"dns.*\.log(\.gz)?$"))
  ;;(clojure.pprint/pprint files)
  
  (if (> (:verbosity options) 0)
    (printf "\n>> DEBUG: Log File count: %s\n" (count files)))

  ;;; print first 10 files
  ;; (if (> (:verbosity options) 0)
  ;;   (do
  ;;     (print ">> DEBUG: Source log-files (first 10):")
  ;;     (println (map #(.getPath %) (take 10 (walk (:path options) #"dns.*\.log(\.gz)?$"))))))
  
  ;;; print a few files
  ;; (clojure.pprint/pprint
  ;;  (map #(.getPath %) (take 2 (walk (:path options) #"dns.*\.log(\.gz)?$"))))

  ;;; the dns logs
  ;; (let [files (walk (:path options) #"dns.*\.log(\.gz)?$")]
  ;;   (clojure.pprint/pprint (take 10 files)))

  ;;;  (print files)
  ;; (def dnsqueries (->>
  ;;                  files
  ;;                  (map #(bro-process-log %))
  ;;                  (first)
  ;;                  (map #(:query %))))
  
  ;; ;;  (clojure.pprint/pprint dnsqueries)
  ;; (clojure.pprint/pprint (frequencies dnsqueries))

  (->> files
       ;; (take 1)
       ;;(map #(clojure.pprint/pprint %))
       (map #(.getPath %))
       (pmap #(bro-process-log %))
       ;; (first)
       ;; (map #(:query %))
       (mapcat (partial map #(:query %)))
       (frequencies)
       ;;       (vals)
       ;;       (reduce +)
       (clojure.pprint/pprint)
       )
  )



  

    ;; (println "attempting (bro-process-log)...")
    ;; (doall (map #(println %) (bro-process-log *file-name*)))

    ;; (clojure.pprint/print-table (bro-process-log "conn.log"))
    ;; (with-out-str (clojure.pprint/pprint (foo "conn.log")))
    ;; (doseq #(-> % str println) (foo** "conn.log"))

;; (def foo (bro-process-log "conn.log"))
;; (apply map println foo)

;;; (trace/trace-ns 'stuff.core)

;;------------
(defn -main
  "do stuff"
  [& args]


  
  (let [*file-name* "conn.log"
        {:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;
    (cond
      (:help options)(exit 0 (usage summary))
      errors (exit 1 (error-msg errors)))

    (if (> (:verbosity options) 0)
      (do
        (clojure.pprint/pprint options)
        (print ">> LOGPATH:" (:path options) \newline)))
    (if (> (:verbosity options) 1)
      (do
        (println ">> DEBUG: Separator:" (bro-log-get-separator connlog))
        (println ">> DEBUG: Fields:" (bro-fields *file-name*))
        (println "\n>> DEBUG: Ten lines from connlog:")
        (dorun (map #(print %) (interleave (repeat "\n>> DEBUG: ") (take 10 connlog))))))
    (if (:test options)
      (do
        (test-function options)))
    )
  )
