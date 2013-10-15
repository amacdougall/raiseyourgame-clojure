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
  ".thumbnail img" (ef/set-attr :src (.-thumbnail video))
  ".thumbnail > a" (ef/set-attr :href (video-link video))
  ".title > a" (ef/do->
                 (ef/content (.-title video))
                 (ef/set-attr :href (video-link video)))
  ".description" (ef/content (.-description video)))

(em/defsnippet home-view (template "video_list.html") ".video-list" [videos]
  ".video-list" (ef/content (map #(video-item %) videos)))

(defn home [context]
  (at ["#content"]
    ; video list is a JS array of JS objects; see issue #1
    (ef/content (home-view (:videos context)))))

(em/defsnippet video-view (template "video.html") ".video" [video]
  ".thumbnail img" (ef/set-attr :src (.-thumbnail video))
  ".thumbnail > a" (ef/set-attr :href (video-link video))
  ".title > a" (ef/do->
                 (ef/content (.-title video))
                 (ef/set-attr :href (video-link video)))
  ".description" (ef/content (.-description video)))

(defn video [context]
  (at ["#content"]
    (ef/content (video-view (:video context)))))
