(ns raiseyourgame.templates
  (:require [enfocus.core :as ef :refer [at]]
            [enfocus.events :as events]
            [enfocus.effects :as effects])
  (:require-macros [enfocus.macros :as em]))

(defn- template [filename]
  (str "/static/templates/" filename))

(em/defsnippet video-item (template "video_list.html") "li.video-item" [video]
  "span.title" (ef/content (.-title video))
  "span.description" (ef/content (.-description video)))

(em/deftemplate home-template (template "video_list.html") [videos]
  "ul.video-list" (ef/content (map #(video-item %) videos)))

(defn home [context]
  (at ["#template"]
    ; video list is a JS array of JS objects; see issue #1
    (ef/content (home-template (:videos context)))))

(defn video [context])
