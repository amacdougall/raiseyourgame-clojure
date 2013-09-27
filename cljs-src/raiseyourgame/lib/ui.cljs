;; Utility functions for DOM interaction and event handling.
;;
;; Heavily based on David Nolen's blog.util.reactive;
;; https://github.com/swannodette/swannodette.github.com/blob/master/code/blog/src/blog/utils/reactive.cljs
(ns raiseyourgame.lib.ui
  (:refer-clojure :exclude [map filter remove distinct concat take-while])
  (:require [cljs.core.async :refer [>! <! chan put! close! timeout]]
            [enfocus.core :as ef :refer [at]]
            [enfocus.events :as events]
            [enfocus.effects :as effects]
            [raiseyourgame.lib.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [enfocus.macros :as em]))

;; Begin listening on the selected element or elements for events of the
;; specified type or types, putting each one in an output channel. Returns the
;; output channel. The optional function can be used for event preprocessing
;; side effects -- most prominently, calling .preventDefault on events before
;; putting them in the channel.
(defn listen
  ([selector type] (listen selector type nil))
  ([selector type f] (listen selector type f (chan)))
  ([selector type f out]
    (at [selector]
      (events/listen type (fn [e]
                            (when f (f e)) ;; side effect, if desired
                            (put! out e))))
   out))

(defn onload [f]
  (set! (.-onload js/window) f))
