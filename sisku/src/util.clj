
(ns util
  (:require [clojure.set])
  (:require [clojure.string])
  (:require [clojure.contrib.string])
  (:require [clojure.contrib.str-utils2])
  (:require [clojure.contrib.duck-streams])
  (:require [clojure.contrib.java-utils])
  (:require [clojure.contrib.math])
  (:import  [java.io BufferedReader InputStreamReader])
  )

(def sqrt clojure.contrib.math/sqrt)
(def ceil clojure.contrib.math/ceil)

(def srepeat clojure.contrib.str-utils2/repeat)
(def sreplace clojure.contrib.str-utils2/replace)
(def scontains? clojure.contrib.str-utils2/contains?)
(def split-lines clojure.contrib.str-utils2/split-lines)
(def lower-case clojure.contrib.str-utils2/lower-case)
(def upper-case clojure.contrib.str-utils2/upper-case)

(def trim clojure.string/trim)
(def trimr clojure.string/trimr)
(def triml clojure.string/triml)

(def stake clojure.contrib.string/take)
(def sdrop clojure.contrib.string/drop)
(def split clojure.contrib.string/split)
(def join clojure.contrib.string/join)

(defn sleep [t] (Thread/sleep t))

(defn read-lines
  ([f]
     (clojure.contrib.duck-streams/read-lines f))
  ([f enc]
     (binding [clojure.contrib.duck-streams/*default-encoding* enc]
       (clojure.contrib.duck-streams/read-lines f))
     )
  )
(def write-lines clojure.contrib.duck-streams/write-lines)
(def slurp* clojure.contrib.duck-streams/slurp*)
(def append-spit clojure.contrib.duck-streams/append-spit)

(def union clojure.set/union)
(def difference clojure.set/difference)
(def intersection clojure.set/intersection)

(defn slice
  ([coll start]
     (let [start (max start 0)]
       (drop start coll)))
  ([coll start end]
     (let [start (max start 0)
           end (if (< end 0) (+ end (count coll)) end)]
       (take (- end start) (drop start coll)))))

(defn sslice
  ([coll start] (apply str (slice coll start)))
  ([coll start end] (apply str (slice coll start end))))

(defn load-lib [class lib]
  (clojure.contrib.java-utils/wall-hack-method
   java.lang.Runtime "loadLibrary0" [Class String]
                    (Runtime/getRuntime) class lib))

(defn log [obj] (println obj) obj)

(defn warn [& ss]
  (binding [*out* *err*]
    (apply println (concat [(. (java.util.Date.) toString) " [WARN] "] ss))
    (if (= (count ss) 1) (first ss) ss)
    )
  )

(defn re-pos [re s]
  (loop [m (re-matcher re s)
         res []]
    (if (.find m)
      (recur m (conj res {:groups (re-groups m) :start (.start m) :end (.end m)}))
      res)))

(defn serialize [o filename]
  (binding [*print-dup* true]
    (clojure.contrib.duck-streams/with-out-writer filename
      (prn o))))

(defn deserialize [filename]
    (read-string (slurp* filename)))

(defn -optparse [m]
  (loop [args m
         opts {}
         prev nil]
    (let [arg (first args)]
      (if-not arg
        (if prev (assoc opts prev true) opts)
        (if (= (first arg) \-)
          (recur (rest args) (if prev (assoc opts prev true) opts) arg)
          (recur (rest args) (if prev (assoc opts prev arg)  opts) nil)
          )
        )
      )
    )
  )

(defn optparse
  ([] (-optparse *command-line-args*))
  ([args] (-optparse args))
  )

(defmacro set-all! [obj m]
    `(do ~@(map (fn [e] `(set! (. ~obj ~(key e)) ~(val e))) m) ~obj))

(defn to-int [x]    (if (integer? x) x (Integer/parseInt x)))
(defn to-double [x] (if (float? x) x (Double/parseDouble x)))

(defn indexed [coll]
  (map-indexed (fn [x y] [y x]) coll)
  )


(defn get-runtime [& p]
  (.. Runtime getRuntime (exec (into-array (filter identity p))))
  )

(defn run-process [& p]
  (let [proc (apply get-runtime p)]
    [(read-lines (.getInputStream proc)) (read-lines (.getErrorStream proc))]
    )  
  )

(defn run-process-wait [& p]
  (let [proc (apply get-runtime p)]
    (. proc waitFor)
    (let [stdout (vec (read-lines (.getInputStream proc)))
          stderr (vec (read-lines (.getErrorStream proc)))]
      (. proc destroy)
      [stdout stderr]
      )
    )  
  )

(defn zshrun [c]
  (run-process-wait "/usr/bin/zsh" "-c" c)
  )

(defn zshrun-all [& cc]
  (let [[stdout stderr] (zshrun (warn (apply str cc)))]
    (if stdout (println (join "\n" stdout)))
    (if stderr (println (join "\n" stderr)))
    [stdout stderr]
    )
  )