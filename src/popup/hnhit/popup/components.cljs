(ns hnhit.popup.components
  (:require [chromex.logging :refer-macros [log]]
            [chromex.ext.tabs :as tabs]
            [re-com.core :as rc]))

;; Chrome stuff
(defn open-passive-tab
  "Open the [url] in a non active tab"
  [url]
  (tabs/create #js {"url" url "active" false}))

(defn open-all-tabs
  "Open all [urls] in new passive tabs"
  [urls]
  (doseq [url urls]
    (open-passive-tab url)))

;; React components
(defn tab-link-cpt [url text]
  [rc/hyperlink
   :label text
   :on-click #(open-passive-tab url)])

(defn story-cpt [item]
  [:p
   [tab-link-cpt (:hn-url item) (:title item)]
   [:span
    {:style {:font-size "0.8em"}}
    (str (:points item) " points. " (:num_comments item) " comments")]])

(defn comment-cpt [item]
  [:p [tab-link-cpt (:hn-url item) (:story_title item)]])

(defn stories-cpt [items]
  [rc/v-box :children
   [
    [rc/title
     :label [rc/h-box
             :gap "10px"
             :children ["Stories"
                        [rc/md-icon-button
                         :md-icon-name "zmdi-open-in-new"
                         :tooltip "Open all stories in new tabs"
                         :on-click #(open-all-tabs (map :hn-url items))]]]
     :level :level2
     :underline? true]
    [:ul
     (for [i items]
       ^{:key i} [:li
                  {:style {:list-style-type "none"}}
                  [story-cpt i]])]]])

(defn related-stories-cpt [items]
  [rc/v-box :children [
                       [rc/title
                        :label "Related"
                        :level :level2
                        :underline? true]
                       [:ul
                        (for [i items]
                          ^{:key i} [:li
                                     {:style {:list-style-type "none"}}
                                     [comment-cpt i]])]]])

(defn loading-cpt
  []
  [rc/box
   :align :center
   :child
   [rc/throbber
    :color "#ff6600"
    :size :large]])

(defn blank-cpt [submit-link]
  [:p
   "No match found. Why don't you "
   [tab-link-cpt submit-link "start the discussion?"]])

(defn error-cpt [http-code]
  [:p (str "Sorry, an error occured (http code " http-code ")")])

(defn hn-cpt [stories related-stories]
  [rc/v-box
   :children [[stories-cpt stories]
              [related-stories-cpt related-stories]]])