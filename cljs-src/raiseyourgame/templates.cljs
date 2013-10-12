(ns raiseyourgame.templates
  (:require [enfocus.core :as ef :refer [at]]
            [enfocus.events :as events]
            [enfocus.effects :as effects])
  (:require-macros [enfocus.macros :as em]))

(defn home [context]
  (at ["#video-list"]
    (ef/content (-> context :videos str))))
;; TODO: actual render of video list; this was a test
