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
(defonce app-state
         (r/atom {:items   []
                  :loading false
                  :url     nil
                  :title   nil
                  }))

(defn loading? [] (:loading @app-state))
(defn loading! [] (swap! app-state assoc :loading true))
(defn finished-loading! [] (swap! app-state assoc :loading false))

(def items-cursor (r/cursor app-state [:items]))

(defn no-results? [] (empty? @items-cursor))

; HN Logic
(def hn-api-search-url "https://hn.algolia.com/api/v1/search?query=")
(def hn-submit-link "https://news.ycombinator.com/submitlink")
(def hn-item-url "https://news.ycombinator.com/item?id=")

(defn build-hn-submit-link []
  (let [url (:url @app-state)
        title (:title @app-state)]
    (str hn-submit-link "?u=" url "&t=" title)))

; TODO handle error response
(defn get-topics [url]
  (loading!)
  (go (let [response (<! (http/get (str hn-api-search-url url)))]
        (swap! app-state assoc :items (get-in response [:body :hits]))
        (finished-loading!))))


(defn build-search-term
  "Remove the http or https term of the url"
  [url]
  (clojure.string/replace url #"^http(s?)://" ""))

(defn search-tab-url []
  (go
    (if-let [[tabs] (<! (tabs/query #js {"active" true "currentWindow" true}))]
      (let [tab (first tabs)
            url (.-url tab)
            title (.-title tab)]
        (swap! app-state assoc :url url :title title)
        (get-topics (build-search-term url))))))

(defn is-story? [item] (nil? (:story_id item)))
(def is-comment? (complement is-story?))

(defn stories []
  (sort-by :points > (filter is-story? @items-cursor)))

(defn related-stories []
  "Return the list of items that matched a comment, distinct by story id"
  (map first (vals (group-by :story_id (filter is-comment? @items-cursor)))))


;; React components
;(defn state-logger-btn []
;  [rc/button
;   :label "Log the state"
;   :on-click #(log @app-state)])

(defn tab-link-cpt [url text]
  [rc/hyperlink
   :label text
   :on-click #((tabs/create #js {"url" url "active" false}))])

(defn story-cpt [item]
  (let [url (str hn-item-url (:objectID item))]
     [:span
      [tab-link-cpt url (:title item)]
      (str (:points item) " points. " (:num_comments item) " comments")]))

(defn comment-cpt [item]
  (let [url (str hn-item-url (:story_id item))]
    [:p [:a {:href url} (:story_title item)]]))

(defn stories-cpt []
  [rc/v-box :children [
                       [rc/title
                        :label "Stories"
                        :level :level2
                        :underline? true]
                       [:ul
                        (for [item (stories)]
                          ^{:key item} [:li
                                        {:style {:list-style-type "none"}}
                                        [story-cpt item]])]]])

(defn related-stories-cpt []
  [rc/v-box :children [
                       [rc/title
                        :label "Related"
                        :level :level2
                        :underline? true]
                       [:ul
                        (for [item (related-stories)]
                          ^{:key item} [:li [comment-cpt item]])]]])

(defn loading-cpt []
  [rc/box
   :align :center
   :child
   [rc/throbber
    :color "#ff6600"
    :size :large
    :style {}
    ]])

(defn blank-cpt []
  [:p
   "No match found. Why don't you "
   [:a {:href (build-hn-submit-link)} "start the discussion?"]])

(defn hn-cpt []
  [rc/v-box
   :children [[stories-cpt]
              [related-stories-cpt]]])

(defn main-cpt []
  [rc/v-box
   :size "auto"
   :children [
              (if (loading?)
                [loading-cpt]
                (if (no-results?)
                  [blank-cpt]
                  [hn-cpt]))]])


(defn frame []
  [rc/scroller
   :v-scroll :auto
   :height "600px"
   :width "600px"
   :padding "10px"
   :style {:background-color "#f6f6ef"}
   :child [main-cpt]])


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
