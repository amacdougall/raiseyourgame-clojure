(ns raiseyourgame
  (:require [cljs.core.async :refer [>! <! chan put!]]
            [secretary.core :as secretary] ;; used by expanded defroute macro
            [raiseyourgame.lib.async :as async]
            [raiseyourgame.lib.ui :as ui]
            [raiseyourgame.lib.history :as history]
            [raiseyourgame.lib.youtube :as youtube]
            [raiseyourgame.controllers :as controllers]
            [clojure.browser.repl])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [secretary.macros :refer [defroute]]))

;; cheap JS URL parsing: an <a> tag has protocol, hostname, etc properties
(defn parse-url [url]
  (let [parser (.createElement js/document "a")]
    (set! (.-href parser) url)
    ;; there may be a nicer way to do this, but whatever
    {:protocol (.-protocol parser)
     :hostname (.-hostname parser)
     :port     (.-port parser)
     :pathname (.-pathname parser)
     :host     (.-host parser)}))

(defn url->pathname [url]
  (-> url parse-url :pathname))

(defn handle-navigation [state]
  (secretary/dispatch! (history/state->route state)))

;; Initial setup
(defn setup []
  (history/bind-navigation-handler handle-navigation)

  ;; push initial state
  (history/push-state (url->pathname (.-location js/window)))

  ;; run initial route
  (secretary/dispatch! (url->pathname (.-location js/window)))

  ;; TODO: combine those in one function?

  ;; set up link navigations
  (let [event->pathname #(-> % .-currentTarget .-href url->pathname)
        clicks (ui/listen-live "a.internal" :click :prevent-default)
        internal-links (async/map event->pathname clicks)]
    (go (loop []
          (history/push-state (<! internal-links))
          (recur))))

  (youtube/init))

;; Secretary routes
(let [home (chan)   ; input channel
      videos (chan) ; input channel
      controller-out (async/fan-in [(controllers/home home)
                                    (controllers/videos videos)])]

  (defroute "/" {:as params}
    (put! home (or params :none)))

  (defroute "/videos" {:as params}
    (put! videos params))

  (defroute "/videos/:id" {:as params}
    (put! videos params))

  (go (loop []
        (when-let [[template context] (<! controller-out)]
          (template context)
          (recur)))))

(ui/onload setup)
