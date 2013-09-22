(ns raiseyourgame
  (:require [cljs.core.async :refer [>! <! chan]]
            [secretary.core :as secretary] ;; used by expanded defroute macro
            [shoreleave.browser.history :as h]
            [raiseyourgame.lib.async :as async]
            [raiseyourgame.lib.ui :as ui]
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

(defn handle-navigation [{:keys [token type navigation]}]
  (secretary/dispatch! (url->pathname token)))

;; jayq/async test
(defn setup []
  (h/navigate-callback handle-navigation)

  ;; load initial state
  (secretary/dispatch! (-> (.-location js/window) parse-url :pathname))

  ;; set up link navigations
  (let [event->pathname #(-> % .-target .-href parse-url :pathname)
        internal-links (->> (ui/listen "a.internal" :click #(.preventDefault %))
                         (async/map event->pathname))]
    (go (loop []
          (let [pathname (<! internal-links)]
            ;; this set-token will trigger h/navigate-callback, which
            ;; triggers secretary/dispatch! in turn.
            (h/set-token! h/history pathname))
          (recur))))
                         
  ;; test jQuery event handlers
  (let [clicks (->> (ui/listen :.jayq-test :click)
                 (async/always :test-click))]
    (go (loop []
          (let [e (<! clicks)]
            (.log js/console (str "Received event: " (name e))))
          (recur)))))

;; Client-side Secretary route test
;; TODO: actual logic on route execution
(defroute "/users/:id/food/:name" {:as params}
  (.log js/console (str "User: " (:id params) " Food: " (:name params))))

(defroute "/users/:id" {:keys [id]}
  (.log js/console (str "User: " id)))

(ui/document-ready setup)
