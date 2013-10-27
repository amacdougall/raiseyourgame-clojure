(ns raiseyourgame.lib.youtube
  (:refer-clojure :exclude [load])
  (:require [clojure.string :refer [split]]
            [cljs.core.async :refer [>! <! chan put! close! timeout]]
            [enfocus.core :as ef :refer [at]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

;; True if the Youtube API is ready for action.
(def api-ready (atom false))

;; Receives a single :ready value when API is ready.
(def api-status (chan))

;; The current embedded player. There can only be one at a time.
(def player (atom nil))

;; True if the Youtube player is ready for action.
(def player-ready (atom false))

(def player-status (chan))

(def player-defaults {:width "640" :height "480"})

(defn- video->id [video]
  (-> video .-url (split #"/") last))

(defn create-player [id]
  (if @api-ready
    ; TODO: if there is an old player, detach and destroy it

    ; This syntax is slightly tortured, but "(.-Player js/YT). foo bar" is
    ; equivalent to "new YT/Player(foo, bar)". After some experimentation, I
    ; learned that ((.-Player js/YT). foo bar) does not work; we need an
    ; intermediate binding to make the runtime happy.
    (let [prototype (.-Player js/YT)
          new-player #(prototype. id (clj->js player-defaults))]
      (swap! player new-player)
      (.addEventListener @player "onReady"
                         (fn []
                           (.log js/console "player ready")
                           (swap! player-ready (constantly true))
                           (put! player-status :ready))))
    (go (loop []
          (when (= (<! api-status) :ready)
            (create-player id))
          (recur)))))

(defn init []
  (let [script (.createElement js/document "script")]
    (at script (ef/set-attr :src "http://www.youtube.com/iframe_api"))
    (at "head" (ef/append script)))

  (set! (.-onYouTubeIframeAPIReady js/window)
        (fn []
          (.log js/console "api ready")
          (swap! api-ready (constantly true))
          (put! api-status :ready))))

;; Given a JS video object with a url property, loads it into the player
;; instance. The loaded video will play automatically.
(defn load [video]
  (if @player-ready
    (.loadVideoById @player (video->id video))
    (go (loop []
          (if (= (<! player-status) :ready)
            (load video)
            (recur))))))

;; Pause the currently loaded video, if any.
(defn pause []
  (when @player-ready
    (.pauseVideo @player)))

;; Play the currently loaded video, if any.
(defn play []
  (when @player-ready
    (.playVideo @player)))

;; Seek to the supplied timecode, in seconds, if video is loaded. If the
;; optional allow-seek-ahead argument is false, a seek location beyond the
;; furthest loaded time will not cause a new video stream request. When
;; performing a single seek operation to start playback at a known location, it
;; is safe to omit this argument, leaving it to default to true.
(defn seek
  ([timecode]
   (seek timecode true))
  ([timecode allow-seek-ahead]
    (when @player-ready
      (.seekTo @player timecode allow-seek-ahead))))
