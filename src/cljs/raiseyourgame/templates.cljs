(ns raiseyourgame.templates
  (:require [raiseyourgame.timeline :as timeline]
            [enfocus.core :as ef :refer [at]]
            [enfocus.events :as events]
            [enfocus.effects :as effects]
            [raiseyourgame.lib.youtube :as youtube])
  (:require-macros [enfocus.macros :as em]))

(defn- template [filename]
  (str "/static/templates/" filename))

(defn- video-link [video]
  (str "/videos/" (:id video)))

(em/defsnippet video-item (template "video_list.html") ".video-item" [video]
  ".image-link img" (ef/set-attr :src (:thumbnail video))
  ".detail-link" (ef/set-attr :href (video-link video))
  ".title" (ef/content (:title video))
  ".description" (ef/content (:description video)))

(em/defsnippet home-view (template "video_list.html") ".video-list" [videos]
  ".video-list" (ef/content (map #(video-item %) videos)))

; SECTION TEMPLATES
; home
(defn home [context]
  (at ["#main-content"] (ef/remove-class "video"))
  (at ["#main-content .container"]
    (ef/content (home-view (:videos context)))))

; video
(em/defsnippet video-view (template "video.html") ".video" [video]
  ".detail .title" (ef/content (:title video))
  ".detail .description p" (ef/content (:description video)))

; annotation; note that we can only operate upon children of .annotation
(em/defsnippet annotation-view (template "video.html") ".annotation" [annotation]
  ".timestamp" (ef/content (timeline/to-timestamp (:time annotation)))
  ".content" (ef/content (:content annotation)))

(defn video [context]
  (let [view (video-view (:video context))]
    (at ["#main-content"] (ef/add-class "video"))
    (at ["#main-content .container"] (ef/content view))
    (at ["#main-content .annotation"] (ef/add-class "hidden"))
    (timeline/equip {:optimize-for-annotations
                     #(at ["#main-content .container"]
                        (at ".description" (ef/add-class "hidden"))
                        (at ".annotation" (ef/remove-class "hidden")))
                     :set-annotation
                     #(at ["#main-content .container .annotation"]
                        (ef/substitute (annotation-view %)))}))
  (youtube/create-player "player")
  (youtube/load (:video context)))
