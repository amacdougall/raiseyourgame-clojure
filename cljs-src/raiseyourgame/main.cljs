(ns raiseyourgame
  (:require [cljs.core.async :refer [>! <! chan]]
            [secretary.core :as secretary] ;; used by expanded defroute macro
            [raiseyourgame.lib.async :as async]
            [raiseyourgame.lib.ui :as ui]
            [raiseyourgame.lib.history :as history]
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

;; jayq/async test
(defn setup []
  (history/bind-navigation-handler handle-navigation)

  ;; push initial state
  (history/push-state (url->pathname (.-location js/window)))

  ;; run initial route
  (secretary/dispatch! (url->pathname (.-location js/window)))

  ;; TODO: combine those in one function?

  ;; set up link navigations
  (let [event->pathname #(-> % .-target .-href url->pathname)
        internal-links (->> (ui/listen "a.internal" :click #(.preventDefault %))
                         (async/map event->pathname))]
    (go (loop []
          (history/push-state (<! internal-links))
          (recur)))))

;; Client-side Secretary route test
;; TODO: actual logic on route execution
(defroute "/" {:as params}
  (.log js/console "root route"))

(defroute "/users/:id/food/:name" {:as params}
  (.log js/console (str "User: " (:id params) " Food: " (:name params))))

(defroute "/users/:id" {:keys [id]}
  (.log js/console (str "User: " id)))

(ui/onload setup)
