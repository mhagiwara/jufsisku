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

  (:use somnium.congomongo)
  )

(def conn
  (make-connection "jufsisku"
                   :host "127.0.0.1"
                   :port 27017))

(def page-parts
  {:meta   [:meta {:http-equiv "Content-Type" :content "text/html; charset=utf-8"}]
   :footer [:hr
            [:div.footer
             [:div
              [:a {:href "."} "home"] "&nbsp;&nbsp;"
              [:a {:href "add"} "add sentence(s)"]]
             [:div "lojbo jufsisku - Lojban sentence search - developed by "
              [:a {:href "http://lilyx.net/"} "Masato Hagiwara"]]]
            ]
   :style [:style {:type "text/css"}
           "\n body {font: 1em helvatica, arial; padding: 0; margin: 0;}
            .header  {background-color: #333333; padding: 16px;}
            h1       {color: #dddddd; margin: 4px;}
            h1 a:link {color: #cccccc; text-decoration: none;}
            h1 a:visited {color: #cccccc; text-decoration: none;}
            h1 a:hover {color: #dddddd; text-decoration: underline;}
            .h1sub   {font-size: 18px;}

            #post_confirm {padding: 18px;}
            #post_confirm #quote {background-color: #cccccc; padding: 8px;
                                  font-style: italic;}

            #search_box {background-color: #666666;}
            #search_box input {margin: 8px}

            #search_res {padding: 18px;}
            #search_res td {padding: 6px;}
            #search_res table   {width: 90%;}
            #search_res td.text {width: 80%;}
            #search_res td.link {width: 20%;}

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

(defn head-html   [params]
  [:head (page-parts :meta)
   [:title (str "lojbo jufsisku - Lojban sentence search"
                (if (params :q) (str ": " (params :q)) ""))]
   (page-parts :style)
   [:script {:type "text/javascript"}
    "
             var _gaq = _gaq || [];
             _gaq.push(['_setAccount', 'UA-175204-10']);
             _gaq.push(['_trackPageview']);

             (function() {
                          var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
                          ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                          var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
                          })();
    "
    ]
   ]
  )

(defn header-html [params]
  [:div
   [:div.header
    [:h1 [:a {:href "."} "lojbo jufsisku"] [:br] [:span.h1sub "Lojban sentence search"]]
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

(defn toppage-html [params]
  [:div#search_res
   [:p
    [:div "Example: "
     [:a {:href "?q=klama"} "klama"] ",&nbsp; "
     [:a {:href "?q=la'e di'u"} "la'e di'u"] ",&nbsp; "
     [:a {:href "?q=store"} "store"] ",&nbsp; "
     [:a {:href "?q=pretty little girls' school"} "pretty little girls' school"]]
    ]
    [:p "You can " [:a {:href "add"} "add more Lojban-English sentences from here"]]
   ]
  )

(defn search-page [params]
  (html
   (doctype :html4)
   [:html
    (head-html params)
    [:body
     (header-html params)
     (if (params :q)
       (solrxml->html
        (str "http://localhost:8983/solr/select/?version=2.2&rows=20&indent=on"
             "&q=jbo_t:" (params :q) " eng_t:" (params :q)
             "&start=" (* (dec (to-int (or (params :p) 1))) 20))
        params)
       (toppage-html params)
       )
     (page-parts :footer)
     ]
    ]
   )
  )

(defn add-page-html [params]
  [:div#search_res
   [:div "Fill out the form below with a Lojban sentence, its English translation, resource ID, and source."
    [:br] "If you'd like to upload many sentences in a batch, " [:a {:href "http://lilyx.net/about-me/"} "please directly contact me."]]
   [:hr]
   [:form {:method "POST" :action "add"}
    [:table
     [:tr
      [:td "Lojban sentence:"]
      [:td [:input {:type "text" :size 60 :name "jbo_t" :value ""}]]
      ]
     [:tr
      [:td] [:td "e.g., &quot; mi klama le zarci &quot;"]
      ]
     [:tr
      [:td "English translation:"]
      [:td [:input {:type "text" :size 60 :name "eng_t" :value ""}]]
      ]
     [:tr
      [:td] [:td "e.g., &quot; I go to the store. &quot;"]
      ]
     [:tr
      [:td "Source:"]
      [:td [:input {:type "text" :size 60 :name "src" :value ""}]]
      ]
     [:tr
      [:td] [:td "e.g., &quot; http://www.example.com/path &quot;" [:br]
             "Specify the URL where the Lojban sentence (optionally with the English translation) appears."
             "Any submittions without source cannot be included in the database because of copyright and validity issues." [:br]
             "If you are submitting sentences from CLL, make it clear which section it appears in (e.g., " [:a {:href "http://dag.github.com/cll/1/3/"} "http://dag.github.com/cll/1/3/"] ")"]
      ]
     [:tr
      [:td "Resource ID:"]
      [:td [:input {:type "text" :size 30 :name "rid" :value ""}]]
      ]
     [:tr
      [:td] [:td "e.g., &quot; CLL &quot; , &quot; jbovlaste &quot;" [:br]
             "(Optional) Specify an unique ID corresponding to the resource where the sentence comes from." [:br]
             "This ID will be shown on the right of search results with unique sentence IDs automatically numbered by the system." [:br]
             "If you are submitting sentences from CLL, just write &quot; CLL &quot;"]
      ]
     ]
    [:input {:type "submit" :value "Submit"}]]
   ]
  )

(defn add-page [params]
  (html
   (doctype :html4)
   [:html
    (head-html params)
    [:body
     (header-html params)
     (add-page-html params)
     (page-parts :footer)
     ]
    ]
   )
  )

(defn add-page-posted [params]
  (with-mongo conn
    (insert! :jufra {:eng_t (params :eng_t)
                     :jbo_t (params :jbo_t)
                     :src   (params :src)
                     :rid   (params :rid)}))
  [:div#post_confirm
   [:p "Thank you for submitting the following sentence!"]
   [:div#quote
    [:div "Lojban sentence:" (params :jbo_t)]
    [:div "English translation:" (params :eng_t)]
    [:div "Source: " (params :src)]
    [:div "Resource ID: " (params :rid)]
    ]
   [:p "After being reviewed manually, your sentence(s) will be added to the database."]
   [:p "You can continue to add more sentence(s) using the following form."]
   [:hr]
   ]
  )

(defn add-page-post [params]
  (html
   (doctype :html4)
   [:html
    (head-html params)
    [:body
     (header-html params)
     (add-page-posted params)
     (add-page-html params)
     (page-parts :footer)
     ]
    ]
   )
  )


(defroutes main-routes
  (GET "/" {params :params} (search-page params))
  (GET "/add" {params :params} (add-page params))
  (POST "/add" {params :params} (add-page-post params))
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
