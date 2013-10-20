(ns raiseyourgame.templates
  (:require [enfocus.core :as ef :refer [at]]
            [enfocus.events :as events]
            [enfocus.effects :as effects])
  (:require-macros [enfocus.macros :as em]))

(defn- template [filename]
  (str "/static/templates/" filename))

(defn- video-link [video]
  (str "/videos/" (.-id video)))

(em/defsnippet video-item (template "video_list.html") ".video-item" [video]
  ".image-link img" (ef/set-attr :src (.-thumbnail video))
  ".detail-link" (ef/set-attr :href (video-link video))
  ".title" (ef/content (.-title video))
  ".description" (ef/content (.-description video)))

(em/defsnippet home-view (template "video_list.html") ".video-list" [videos]
  ".video-list" (ef/content (map #(video-item %) videos)))

(defn home [context]
  (at ["#main-content .container"]
    ; video list is a JS array of JS objects; see issue #1
    (ef/content (home-view (:videos context)))))

; TODO: this will have to be considerably more complex to include
; annotations, if nothing else
(em/defsnippet video-view (template "video.html") ".video" [video]
  ".detail .title" (ef/content (.-title video))
  ".detail .description p" (ef/content (.-description video)))

(defn video [context]
  (at ["#main-content .container"]
    (ef/content (video-view (:video context)))))
