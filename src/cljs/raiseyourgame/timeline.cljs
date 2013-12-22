(ns raiseyourgame.timeline
  (:require [raiseyourgame.lib.youtube :as youtube])
  (:require-macros [raiseyourgame.lib.macros :refer [dochan]]))

(def ui (atom {:set-annotation nil}))

;; Given a map of key-value pairs, or one key and one value, updates the ui
;; atom with the new functions.
(defn equip
  ([m] (swap! ui merge m))
  ([k v] (swap! ui assoc k v)))

(defn- ui-run [k & args]
  (if-let [f (@ui k)]
    (if (nil? args) (f) (apply f args))
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
    ; If current annotation was reached, but next one was not, return it; if
    ; none are left to check, return nil; otherwise, try next annotation.
    (cond
      (and (reached t current) (not (reached t (first script)))) current
      (empty? script) nil
      :else (recur t (first script) (rest script)))))

(defn run [script]
  (let [optimized false
        optimize #(ui-run :optimize-for-annotations)
        annotate #(ui-run :set-annotation (locate % script))]
    (youtube/on-timecode optimize :once)
    (youtube/on-timecode annotate)))

;; Given a time in seconds, returns a formatted string in "m:ss" format.
(defn to-timestamp [t]
  (let [m (int (/ t 60))
        s (int (mod t 60))
        pad (when (< s 10) "0")]
    (str m ":" pad s))) ; str ignores nil
