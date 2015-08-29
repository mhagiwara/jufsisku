
(ns misc.sisku.tsv2xml
  (:use util)
  )

(println "<add>")
(doseq [line (read-lines *in*)]
  (let [[tid id jbo eng src] (split #"\t" line)]
    (println "<doc>")
    (println (str "<field name=\"id\">" tid ":" id "</field>"))
    (println (str "<field name=\"jbo_t\">" jbo "</field>"))
    (println (str "<field name=\"eng_t\">" eng "</field>"))
    (println (str "<field name=\"src_t\">" src "</field>"))
    (println "</doc>")
    )
  )
(println "</add>")
