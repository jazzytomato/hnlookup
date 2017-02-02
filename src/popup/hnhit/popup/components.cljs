(ns hnhit.popup.components
  (:require [chromex.logging :refer-macros [log]]
            [chromex.ext.tabs :as tabs]
            [cljsjs.moment]
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
  (let [{:keys [title hn-url points author num_comments created_at]} item]
    [rc/v-box
     :children
     [[tab-link-cpt hn-url title]
      [rc/h-box
       :style {:font-size "0.8em"}
       :children
       [(let [time-ago (-> (js/moment created_at) .fromNow)]
        (str points " points by " author ". " time-ago " | " num_comments " comments"))]]]]))

(defn comment-cpt [item]
  [tab-link-cpt (:hn-url item) (:story_title item)])

(defn title-open-tab-cpt [label items]
  [rc/title
   :label [rc/h-box
           :gap "10px"
           :children [label
                      [rc/md-icon-button
                       :md-icon-name "zmdi-open-in-new"
                       :tooltip "Open all stories in new tabs"
                       :on-click #(open-all-tabs (map :hn-url items))]]]
   :level :level2
   :underline? true])

(defn stories-cpt [items]
  [rc/v-box :children
   [[title-open-tab-cpt "Hits" items]
    [rc/v-box
     :gap "5px"
     :children
     [(for [i items]
       ^{:key i} [story-cpt i])]]]])

(defn related-stories-cpt [items]
  [rc/v-box :children
   [[title-open-tab-cpt "Related" items]
     [rc/v-box
      :children
      [(for [i items]
        ^{:key i} [comment-cpt i])]]]])

(defn loading-cpt
  []
  [rc/box
   :align :center
   :child
   [rc/throbber
    :color "#ff6600"
    :size :large]])

;; icons are available here http://zavoloklom.github.io/material-design-iconic-font/icons.html
(defn blank-cpt [submit-link]
  [rc/v-box
   :align :center
   :gap "10px"
   :children
   [[rc/md-icon-button :md-icon-name "zmdi-help-outline"]
    [tab-link-cpt submit-link "No match found. Click to start the discussion!"]]])

(defn error-cpt [http-code]
  [rc/v-box
   :align :center
   :gap "10px"
   :children
   [[rc/md-icon-button :md-icon-name "zmdi-alert-polygon"]
    [rc/label :label (str "Sorry, an error occured. (http code " http-code ")")]]])

(defn repost-cpt [submit-link]
  [rc/hyperlink
   :on-click #(open-passive-tab submit-link)
   :tooltip "This page is eligible for a repost (no high or recent activity)"
   :label [rc/h-box
           :gap "5px"
           :justify :center
           :children
           [[rc/md-icon-button :md-icon-name "zmdi-info-outline" :size :smaller]
            [:em "Repost"]
            ]]])

(defn hn-cpt [stories related-stories]
    [rc/v-box
     :children [[stories-cpt stories]
                [related-stories-cpt related-stories]]])
