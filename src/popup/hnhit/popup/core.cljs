(ns hnhit.popup.core
  (:import [goog.dom query])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [chromex.ext.tabs :as tabs]
            [goog.dom :as gdom]
            [reagent.core :as r]
            [re-com.core :as rc]
            [hnhit.popup.components :as cpts]
            [cljsjs.moment]))

(defonce app-state
  (r/atom {:items   []
           :loading false
           :error   nil
           :url     nil
           :title   nil
           :search-terms []}))

(def items-cursor (r/cursor app-state [:items]))

(defn results? [] (seq @items-cursor))
(def no-results? (complement results?))
(defn error? [] (some? (:error @app-state)))
(defn loading? [] (:loading @app-state))
(defn loading! [] (swap! app-state assoc :loading true :error nil))
(defn finished-loading! [] (swap! app-state assoc :loading false))

(def hn-api-search-url "https://hn.algolia.com/api/v1/search")
(def hn-submit-link "https://news.ycombinator.com/submitlink")
(def hn-item-url "https://news.ycombinator.com/item?id=")

(defn repost-allowed? [stories]
  (let [last-post-date (js/moment (:created_at (apply max-key :created_at_i stories)))
        total-points (apply + (map :points stories))]
    (and (> (.. (js/moment) (diff last-post-date "months")) 8)
         (< total-points 250))))

(defn is-story? [item] (nil? (:story_id item)))
(def is-comment? (complement is-story?))

(defn build-hn-url [item]
  (str hn-item-url (item (if (is-story? item)
                           :objectID
                           :story_id))))

(defn transform-response [m]
  "Maps the relevant URL for each item of the reponse and returns the array of items"
  (let [hits (get-in m [:body :hits])]
    (map #(assoc % :hn-url (build-hn-url %)) hits)))

(defn build-hn-submit-link
  "Build a submit link based on the current tab url and title"
  []
  (let [url (:url @app-state)
        title (:title @app-state)]
    (str hn-submit-link "?u=" url "&t=" title)))

(defn build-search-terms
  "Sanitize url and returns an array of search terms. i.e.
   the url https://www.domain.com/abcd/1234?q=query would return the vector
   'www.domain.com/abcd/1234' 'www.domain.com' '/abcd/1234'"
  [s]
  (drop 1 (re-find #"^https?\://(([^/]+)([^\r\n\#]*)?)" s)))

(defn hn-api-search
  "Queries the HN Api and update the state with results."
  [s]
  (loading!)
  (go (let [response (<! (http/get hn-api-search-url {:query-params {"query" s}}))]
        (if (= (:status response) 200)
          (swap! app-state assoc :items (transform-response response))
          (swap! app-state assoc :error (:status response)))
        (finished-loading!))))

(defn search-tab-url
  "Get the current tab url and update the state"
  []
  (go
    (if-let [[tabs] (<! (tabs/query #js {"active" true "currentWindow" true}))]
      (let [tab (first tabs)
            tab-url (.-url tab)
            title (.-title tab)]
        (if-let [search-term (first (build-search-terms tab-url))]
          ((swap! app-state assoc :url tab-url :title title)
            (hn-api-search search-term)))))))

(defn list-stories
  "Return the list of stories ordered by points desc"
  []
  (sort-by :points > (filter is-story? @items-cursor)))

(defn list-related-stories
  "Return the list of items that matched a comment, distinct by story id"
  []
  (map first (vals (group-by :story_id (filter is-comment? @items-cursor)))))

;; React components
(defn main-cpt []
  (let [submit-link (build-hn-submit-link)
        s (list-stories)
        rs (list-related-stories)]
    [rc/v-box
     :size "auto"
     :children
     [(if (error?)
        [cpts/error-cpt (:error @app-state)]
        (if (loading?)
          [cpts/loading-cpt]
          (if (no-results?)
            [cpts/blank-cpt submit-link]
            [rc/v-box
             :size "auto"
             :gap "10px"
             :children
             [[cpts/hn-cpt s rs]
              [rc/line]
              (when (repost-allowed? s)
                [cpts/repost-cpt submit-link])]])))]]))

(defn frame-cpt []
  [rc/scroller
   :v-scroll :auto
   :height "600px"
   :width "600px"
   :padding "10px"
   :style {:background-color "#f6f6ef"}
   :child [main-cpt]])

(defn mountit []
  (r/render [frame-cpt] (aget (query "#main") 0)))

;(defn process-message! [message]
;  (log "POPUP: got message:" message))
;
;(defn run-message-loop! [message-channel]
;  (log "POPUP: starting message loop...")
;  (go-loop []
;           (when-let [message (<! message-channel)]
;             (process-message! message)
;             (recur))
;           (log "POPUP: leaving message loop")))

;(defn connect-to-background-page! []
;  (let [background-port (runtime/connect)]
;    (run-message-loop! background-port)))


; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (mountit)
  (if (no-results?)
    (search-tab-url)))
