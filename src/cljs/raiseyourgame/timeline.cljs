(ns raiseyourgame.timeline
  (:require [raiseyourgame.lib.youtube :as youtube])
  (:require-macros [raiseyourgame.lib.macros :refer [dochan]]))

(def ui (atom {:set-annotation nil}))

;; Given a map of key-value pairs, or one key and one value, updates the ui
;; atom with the new functions.
(defn equip
  ([m] (swap! ui merge m))
  ([k v] (swap! ui assoc k v)))

(defn- ui-run [k arg]
  (if-let [f (@ui k)]
    (f arg)
    (throw (js/Error. (str "Attempted to run function for unknown key " k)))))

(defn- distance [t annotation]
  (Math/abs (- t (:time annotation))))

(defn- locate
  ([t script] (locate t nil script))
  ([t nearest script]
    (cond
      (empty? script)
      nearest

      (or (nil? nearest)
          (< (distance t (first script)) (distance t nearest)))
      (recur t (first script) (rest script))

      :else
      (recur t nearest (rest script)))))

(defn run [script]
  (youtube/on-timecode
    (fn [t]
      (let [annotation (locate t script)]
        (.log js/console "Annotation at %s: %s" t (:text annotation))
        (ui-run :set-annotation (locate t script))))))
