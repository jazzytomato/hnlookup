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
            [re-com.core :as rc]))

; Popup state
(defonce popup-state
  (r/atom {:items []}))

; HN Logic
(def hn-api-search-url "https://hn.algolia.com/api/v1/search?query=")
(def hn-submit-link "https://news.ycombinator.com/submitlink?u=")
(def hn-item-url "https://news.ycombinator.com/item?id=")

(defn get-topics [url]
    (go (let [response (<! (http/get (str hn-api-search-url url)))]
      (swap! popup-state assoc-in [:items] (get-in response [:body :hits])))))


(defn build-search-term
  "Remove the http or https term of the url"
  [url]
  (clojure.string/replace url #"^http(s?)://" "")
  )

(defn search-tab-url []
    (go
    (if-let [[tabs] (<! (tabs/query #js {"active" true "currentWindow" true}))]
      (get-topics (build-search-term(.-url (first tabs)))))))

(defn is-story? [item]
  (nil? (:story_id item)))

(def is-comment? (complement is-story?))

;; React components
(defn state-logger-btn []
  [rc/button
    :label "Log the state"
    :on-click #(log @popup-state)])

(defn story-cpt [item]
  (let [url (str hn-item-url (:objectID item))]
    [:p (str "[" (:points item) "] ")
      [:a {:href url} (:title item)]
      (str " (" (:num_comments item) " comments)")]))

(defn comment-cpt [item]
  (let [url (str hn-item-url (:story_id item))]
    [:p [:a {:href url} (:story_title item)]]))

(defn stories-cpt []
  [rc/v-box :children [
    [:h2 "Hacker news stories:"]
    [:ul
     (for [item (sort-by :points > (filter is-story? (:items @popup-state)))]
       ^{:key item} [:li (story-cpt item)])]]])

(defn related-stories-cpt []
  [rc/v-box :children [
    [:h2 "Related"]
    [:ul
     (for [item (filter is-comment? (:items @popup-state))]
     ^{:key item} [:li (comment-cpt item)])]]])

(defn main-hn-cpt []
  (if (empty? (:items @popup-state))
    [rc/throbber]
    [rc/v-box
     :children [[state-logger-btn]
                [stories-cpt]
                [related-stories-cpt]]]))


(defn frame []
  [rc/scroller
   :v-scroll :auto
   :height   "600px"
   :width    "600px"
   :child    [main-hn-cpt]])


(defn mountit []
  (r/render [frame] (aget (query "#main") 0)))

(defn process-message! [message]
  (log "POPUP: got message:" message))

(defn run-message-loop! [message-channel]
  (log "POPUP: starting message loop...")
  (go-loop []
    (when-let [message (<! message-channel)]
      (process-message! message)
      (recur))
    (log "POPUP: leaving message loop")))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port "hello from POPUP!")
    (run-message-loop! background-port)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "POPUP: init")
  (mountit)
  (search-tab-url)
  (connect-to-background-page!))

