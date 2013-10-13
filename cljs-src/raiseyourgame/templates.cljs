(ns raiseyourgame.templates
  (:require [enfocus.core :as ef :refer [at]]
            [enfocus.events :as events]
            [enfocus.effects :as effects])
  (:require-macros [enfocus.macros :as em]))

(defn home [context]
  (at ["#video-list li.video-item"]
    ; video list is a JS array of JS objects; see issue #1
    (em/clone-for [video (:videos context)]
      "span.title" (ef/content (.-title video))
      "span.description" (ef/content (.-description video)))))

(defn video [context])
