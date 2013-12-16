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

;; True if the timecode is later than the annotation time; i.e. the
;; annotation has been reached.
(defn- reached [t annotation]
  (if (nil? annotation)
    false
    (> (- t (:time annotation)) 0)))

(defn- locate
  ([t script] (locate t nil script))
  ([t current script]
    ; If current annotation was reached, return it; if none are left to
    ; check, return nil; otherwise, try next annotation in script.
    (cond
      (reached t current) current
      (empty? script) nil
      :else (recur t (first script) (rest script)))))

(defn run [script]
  (youtube/on-timecode
    (fn [t]
      (let [annotation (locate t script)]
        (.log js/console "Annotation at %s: %s" t (:text annotation))
        (ui-run :set-annotation (locate t script))))))
