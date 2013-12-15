(ns raiseyourgame.lib.youtube
  (:refer-clojure :exclude [load])
  (:require [raiseyourgame.lib.async :as async]
            [clojure.string :refer [split]]
            [cljs.core.async :refer [>! <! chan put! close! timeout]]
            [enfocus.core :as ef :refer [at]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [raiseyourgame.lib.macros :refer [dochan]]))

;; Possible Youtube player states. When the onStateChange event occurs, the
;; data property of the event object will be one of these integers. This map
;; just duplicates YT.PlayerState for convenience. Note that since the keys are
;; the integers, you must address the map by the event constants, but since we
;; only expect to use it in a (= (states (.-data event)) ...) form, this is
;; actually convenient.
(def states {-1 :unstarted
              0 :ended
              1 :playing
              2 :paused
              3 :buffering
              5 :cued})

;; Possible Youtube error values. As above.
;; See https://developers.google.com/youtube/iframe_api_reference#Events.
(def errors {  2 :invalid-parameter ; such as a bad video id
               5 :cannot-use-html5  ; HTML5-related errors
             100 :not-found         ; also occurs if video has been marked private
             101 :embed-not-permitted
             150 :embed-not-permitted})

;; Poll for the current timecode every 250ms during playback. Actual
;; timecode granularity depends on the video; in practice, not all videos
;; will have keyframes every quarter-second.
(def timecode-frequency 250)

;; True if the Youtube API is ready for action.
(def api-ready (atom false))

;; Receives a single :ready value when API is ready.
(def api-status (chan))

;; The current embedded player. There can only be one at a time.
(def player (atom nil))

;; True if the Youtube player is ready for action.
(def player-ready (atom false))

(def ready-states (chan))

(def player-defaults {:width "640" :height "480"})

;; The currently loaded video. There can only be one at a time.
(def loaded-video (atom {}))

;; Called on an interval when player state is :playing.
(defn- handle-timecode []
  (async/publish :timecodes (.getCurrentTime @player)))

(defn on-timecode [f]
  (let [timecodes (async/subscribe :timecodes)]
    (dochan [timecode timecodes]
      (f timecode))))

;; Extracts a video id from a video URL of the expected type. This will
;; probably have to be improved if we get heterogeneous URLs, which we probably
;; will. Maybe it would be better to extract the video ids at the video posting
;; stage instead of CLJS, though.
(defn- url->id [url]
  (let [by-regex #(second (re-find % url))]
    (or (by-regex #"youtu\.be/(\w+)")
        (by-regex #"youtube\.com/watch\?.*v=(\w+)\b"))))

(defn- handle-ready []
  (.log js/console "player ready")
  (reset! player-ready true)
  (put! ready-states :ready))

;; Handle player state change by publishing the matching state keyword to the
;; :player-states channels. Note that this is a state keyword from the states
;; hash, not the integer given by the Youtube API.
(defn- handle-state-change [event]
  (async/publish :player-states (states (.-data event))))

(defn on-state-change [f]
  (let [player-states (async/subscribe :player-states)]
    (dochan [state player-states]
      (f state))))

;; Handles an error simply by throwing it with its keyword as the only message.
;; We may improve this later.
(defn- handle-error [event]
  (throw (js/Error. (str "ERROR: " (name (errors (.-data event)))))))

(defn create-player [id]
  (if @api-ready
    ; TODO: if there is an old player, detach and destroy it
    ; TODO: detach and destroy the player when navigating away from the video
    ; detail view?

    ; This syntax is slightly tortured, but "(.-Player js/YT). foo bar" is
    ; equivalent to "new YT/Player(foo, bar)". After some experimentation, I
    ; learned that ((.-Player js/YT). foo bar) does not work; we need an
    ; intermediate binding to make the runtime happy.
    (let [prototype (.-Player js/YT)
          new-player #(prototype. id (clj->js player-defaults))]
      (swap! player new-player) ; TODO: use reset! instead?
      (.addEventListener @player "onReady" handle-ready)
      (.addEventListener @player "onStateChange" handle-state-change)
      (.addEventListener @player "onError" handle-error))
    (go (loop []
          (when (= (<! api-status) :ready)
            (create-player id))
          (recur)))))

(defn init []
  ; Append Youtube iframe API script to <head>, so it will be loaded.
  (let [script (.createElement js/document "script")]
    (at script (ef/set-attr :src "http://www.youtube.com/iframe_api"))
    (at "head" (ef/append script)))

  ; When iframe API is ready, send it down the channel.
  (set! (.-onYouTubeIframeAPIReady js/window)
        (fn []
          (.log js/console "api ready")
          (swap! api-ready (constantly true))
          (put! api-status :ready)))

  ; Process timecode events; we can start waiting on this channel
  ; before the player even exists, because channels are great.
  (on-timecode
    ; TODO: handle timecodes by displaying annotations
    #(.log js/console "timecode: %s" %))

  (let [interval (atom nil)]
    (on-state-change
      (fn [state]
        (.log js/console "state change: %s; interval handle %d; video url %s"
              (name state) (or @interval "nil") (.getVideoUrl @player))
        (when (not (nil? @interval))
          (js/clearInterval @interval))
        ; start polling for timecodes if playback has begun, and the current video
        ; has the same id as the loaded video
        (if (and (= state :playing)
                 (= (-> @player .getVideoUrl url->id)
                    (-> @loaded-video :url url->id)))
          (reset! interval (js/setInterval handle-timecode timecode-frequency))
          (reset! interval nil))))))

;; Given a JS video object with a url property, loads it into the player
;; instance. The loaded video will play automatically.
(defn ^:export load [video]
  (.log js/console "youtube/load: @player-ready %s" @player-ready)
  (if @player-ready
    (do
      (reset! loaded-video video)
      (.loadVideoById @player (-> video :url url->id)))
    ; TODO: come up with a strategy to handle repeated load calls
    ; for different videos. Right now all the go loops will remain
    ; in effect!
    (go (loop []
          (if (= (<! ready-states) :ready)
            (load video)
            (recur))))))

;; Pause the currently loaded video, if any.
(defn ^:export pause []
  (when @player-ready
    (.pauseVideo @player)))

;; Pause the currently loaded video. If the video is not currently playing, and
;; not currently paused -- that is, if it is loading or seeking -- pause it as
;; soon as possible. This may have the result of countermanding user input, so
;; use it sparingly if at all. Originally written as a test cleanup function.
(defn ^:export force-pause []
  (let [deferred-pause
        (fn []
          (let [player-states (async/subscribe :player-states)]
            (go (loop []
                  (condp = (<! player-states)
                    :playing (pause)
                    :paused nil ; exit silently
                    (recur))))))]
    (if @player-ready
      (condp = (states (.getPlayerState @player))
        :playing (pause)
        :paused nil ; exit silently)
        (deferred-pause))
      (deferred-pause)))) ; kind of gross, but it works for now

;; Play the currently loaded video, if any.
(defn ^:export play []
  (when @player-ready
    (.playVideo @player)))

;; Seek to the supplied timecode, in seconds, if video is loaded. If the
;; optional allow-seek-ahead argument is false, a seek location beyond the
;; furthest loaded time will not cause a new video stream request. When
;; performing a single seek operation to start playback at a known location, it
;; is safe to omit this argument, leaving it to default to true.
(defn ^:export seek
  ([timecode]
   (seek timecode true))
  ([timecode allow-seek-ahead]
    (when @player-ready
      (.seekTo @player timecode allow-seek-ahead))))
