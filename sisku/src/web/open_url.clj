(ns web.open-url
  (:use util)
  (:import (java.net URL)
           (java.lang StringBuilder)
           (java.io BufferedReader InputStreamReader))
  )

(defn open-url
  [address encoding]
  (let [url (URL. address)]
    (with-open [stream (. url (openStream))]
      (let [buf (BufferedReader. (InputStreamReader. stream encoding))]
        (join "\n" (line-seq buf))))))

; (println (open-url "http://lang-8.com/206539/" "UTF-8"))