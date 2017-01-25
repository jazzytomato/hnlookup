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
            [hnhit.popup.components :as cpts]))

(defonce app-state
         (r/atom {:items   []
                  :loading false
                  :error   nil
                  :url     nil
                  :title   nil
                  }))


(def items-cursor (r/cursor app-state [:items]))

(defn no-results? [] (empty? @items-cursor))
(defn error? [] (some? (:error @app-state)))
(defn loading? [] (:loading @app-state))
(defn loading! [] (swap! app-state assoc :loading true :error nil))
(defn finished-loading! [] (swap! app-state assoc :loading false))

(def hn-api-search-url "https://hn.algolia.com/api/v1/search?query=")
(def hn-submit-link "https://news.ycombinator.com/submitlink")
(def hn-item-url "https://news.ycombinator.com/item?id=")

(defn is-story? [item] (nil? (:story_id item)))
(def is-comment? (complement is-story?))

(defn build-hn-url [item]
  (str hn-item-url (item (if (is-story? item)
                           :objectID
                           :story_id))))


(defn transform-response [r]
  "Maps the relevant URL for each item of the reponse and returns the array of items"
  (let [hits (get-in r [:body :hits])]
    (map #(assoc % :hn-url (build-hn-url %)) hits)))

(defn get-topics
  "Queries the HN Api and update the state with results."
  [url]
  (loading!)
  (go (let [response (<! (http/get (str hn-api-search-url url)))]
        (if (= (:status response) 200)
          (swap! app-state assoc :items (transform-response response))
          (swap! app-state assoc :error (:status response)))
        (finished-loading!))))


(defn build-hn-submit-link
  "Build a submit link based on the current tab url and title"
  []
  (let [url (:url @app-state)
        title (:title @app-state)]
    (str hn-submit-link "?u=" url "&t=" title)))

(defn build-search-term
  "Remove the http or https term of the url"
  [url]
  (clojure.string/replace url #"^http(s?)://" ""))

(defn search-tab-url
  "Get the current tab url and update the state"
  []
  (go
    (if-let [[tabs] (<! (tabs/query #js {"active" true "currentWindow" true}))]
      (let [tab (first tabs)
            url (.-url tab)
            title (.-title tab)]
        (swap! app-state assoc :url url :title title)
        (get-topics (build-search-term url))))))

(defn stories
  "Return the list of stories ordered by points desc"
  []
  (sort-by :points > (filter is-story? @items-cursor)))

(defn related-stories
  "Return the list of items that matched a comment, distinct by story id"
  []
  (map first (vals (group-by :story_id (filter is-comment? @items-cursor)))))


;; React components
(defn main-cpt []
  [rc/v-box
   :size "auto"
   :children
   [(if (error?)
      [cpts/error-cpt (:error @app-state)]
      (if (loading?)
        [cpts/loading-cpt]
        (if (no-results?)
          [cpts/blank-cpt (build-hn-submit-link)]
          [cpts/hn-cpt (stories) (related-stories)])))]])

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
  (search-tab-url))
