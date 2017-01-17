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
            [reagent.core :as r]))

; Popup state
(defonce popup-state
  (r/atom
    {:url "Nothing yet"
     :items []}
    ))

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

;; React components
(defn state-logger-btn []
  [:button {:on-click #(log @popup-state)} "Log the state"])

(defn item-cpt [item]
  (let [url (str hn-item-url (:objectID item))]
    [:span (str "[" (:points item) "] ")
      [:a {:href url} (:title item)]
      (str " (" (:num_comments item) ")")]
  ))

(defn frame []
  [:div {:style {:width "600px" :height "400px"}}
   [:p (:url @popup-state)]
   [:ul
   (for [item (sort-by :points > (:items @popup-state))]
     ^{:key item} [:li (item-cpt item)])]
   [state-logger-btn]])

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

