(defproject stuff "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"],
                 [clojure-csv/clojure-csv "2.0.1"],
                 [clj-mmap "1.1.2"],
                 [semantic-csv "0.1.0"],
                 [org.clojure/tools.cli "0.3.5"]
                 [org.apache.commons/commons-compress "1.10"]
                 [org.tukaani/xz "1.5"]
                 [org.clojure/tools.trace "0.7.9"]
                 ]
  :main ^:skip-aot stuff.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             }
  :jvm-opts ["-Xmx2g"
             "-Xms2g"
             "-XX:NewRatio=3"
             "-Xloggc:gc.log"
             "-XX:+UnlockExperimentalVMOptions"
             "-XX:+UseG1GC"
             "-XX:+PrintGCDateStamps"
             "-server"])
