(ns misc.sisku.core
  (:gen-class)
  (:use util)
  (:use compojure.core, ring.adapter.jetty)
  (:use web.open-url)

  (:use hiccup.core)
  (:use hiccup.page-helpers)
  (:use hiccup.form-helpers)

  (:use clojure.xml)
  
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:require [clojure.contrib.logging :as logging])
  )

(def page-parts
  {:meta   [:meta {:http-equiv "Content-Type" :content "text/html; charset=utf-8"}]
   :title  [:title "lojbo jufsisku - Lojban sentence search"]
   :footer [:hr
            [:div.footer "lojbo jufsisku - Lojban sentence search - developed by Masato Hagiwra"]
            ]
   :style [:style {:type "text/css"}
           "\n body {font: 1em helvatica, arial; padding: 0; margin: 0;}
            .header  {background-color: #333333; padding: 16px;}
            h1       {color: #dddddd; margin: 4px;}
            .h1sub   {font-size: 18px;}

            #search_box table   {width: 100%;}
            #search_box td.text {width: 80%;}
            #search_box td.link {width: 20%;}
            #search_box {background-color: #666666;}
            #search_box input {margin: 8px}
            #search_res {padding: 18px;}
            #search_res td {padding: 6px;}

            .jbo_t   {color: #000000; font-family: Lucida Console, monospace;}
            .eng_t   {color: #666666;}
            .id      {font-size: 0.8em;}
            .pg      {margin-left: 2px; margin-right: 2px;}
            .pg_current {font-weight: bold; margin-left: 2px; margin-right: 2px; }

            .footer  {padding: 8px;}
            "
           ]
   }
  )

(defn header-html [params]
  [:div
   [:div.header
    [:h1 [:span "lojbo jufsisku"] [:br] [:span.h1sub "Lojban sentence search"]]
    ]
   [:div#search_box
    [:form {:method "GET" :action "."}
     [:input {:type "text" :size 30 :name "q" :value (or (params :q) "")}]
     [:input {:type "submit" :value "Search"}]]
    ]
   ]
  )

(defn pagenation-html [params num-found]
  (let [p (to-int (or (params :p) 1))
        total-ps (ceil (/ num-found 20))
        width 3
        qstr (str "?q=" (params :q))]
    (if (> total-ps 1)
      [:div.pagenation
       (if (not= p 1)
         [:span.pg [:a {:href (str qstr "&p=" (dec p) )} "&lt;&lt;prev"]])
       (for [i (range (max 1 (- p width))
                      (min (inc total-ps)
                           (max (+ p width) (+ 1 (max 1 (- p width)) (* width 2)))))]
         
         (if (= i p)
           [:span.pg_current i]
           [:span.pg
            [:a {:href (str qstr "&p=" i)} i]])
         )
       (if (not= p total-ps)
         [:span.pg [:a {:href (str qstr "&p=" (inc p))} "next&gt;&gt;"]])
       ]
      )
    )
  )

(defn solrxml->html [file params]
  (let [xml (parse file)
        result (first (filter #(= (:tag %) :result) (xml-seq xml)))
        {{num-found :numFound} :attrs} result]
;    (logging/info (str "solr result: " xml))
    [:div#search_res
     [:div (str "Total: " num-found " result(s)")]
     [:table
      (for [x (xml-seq result) :when (= (:tag x) :doc)]
        (let [[{[eng_t] :content} {[id] :content}
               {[jbo_t] :content} {[src] :content}] (:content x)]
          [:tr
           [:td.text
            [:div.jbo_t jbo_t] [:div.eng_t eng_t]
            ]
           [:td.link
            [:div.id [:a {:href src} id]]
            ]
           ]
          )
        )
      ]
     (pagenation-html params (to-int num-found))
     ]
    )
  )

(defn search-page [params]
  (html
   (doctype :html4)
   [:html
    [:head (page-parts :meta) (page-parts :title) (page-parts :style)]
    [:body
     (header-html params)
     (if (params :q)
       (solrxml->html
        (str "http://localhost:8983/solr/select/?version=2.2&rows=20&indent=on"
             "&q=" (params :q)
             "&start=" (* (dec (to-int (or (params :p) 1))) 20))
        params)        
       )
     (page-parts :footer)
     ]
    ]
   )
  )

(defroutes main-routes
  (GET "/" {params :params} (search-page params))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(defn wrap-charset [handler charset]
  (fn [request]
    (if-let [response (handler request)]
      (if-let [content-type (get-in response [:headers "Content-Type"])]
        (if (.contains content-type "charset")
          response
          (assoc-in response
                    [:headers "Content-Type"]
                    (str content-type "; charset=" charset)))
        response))))

(defn -main [& args]
  (run-jetty (-> main-routes
                 (handler/site)
                 (wrap-charset "utf-8")) {:port 8080})
  )
