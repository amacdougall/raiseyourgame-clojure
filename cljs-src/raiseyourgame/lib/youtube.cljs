(ns raiseyourgame.lib.youtube
  (:require [cljs.core.async :refer [>! <! chan put! close! timeout]]
            [enfocus.core :as ef :refer [at]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

;; The current embedded player. There can only be one at a time.
(def player (atom nil))

(def player-defaults {:width "640" :height "480"})

(defn- create-player [id]
  (fn []
    ; This syntax is slightly tortured, but "(.-Player js/YT). foo bar" is
    ; equivalent to "new YT/Player(foo, bar)". After some experimentation, I
    ; learned that ((.-Player js/YT). foo bar) does not work; we need an
    ; intermediate binding to make the runtime happy.
    (let [ctor (.-Player js/YT)
          new-player #(ctor. "player" (clj->js player-defaults))
          status (chan)]
      (swap! player new-player)
      (.addEventListener @player "onReady"
                         #(.log js/console "player.onReady")))))

(defn init []
  (let [script (.createElement js/document "script")]
    (at script (ef/set-attr :src "https://www.youtube.com/iframe_api"))
    (at "head" (ef/append script)))

  (set! (.-onYouTubeIframeAPIReady js/window) (create-player "player")))

(defn load-video [url]
  (.loadVideoById @player url))
