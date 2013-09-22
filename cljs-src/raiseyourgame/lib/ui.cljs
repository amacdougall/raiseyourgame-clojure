;; Utility functions for DOM interaction and event handling.
;;
;; Heavily based on David Nolen's blog.util.reactive;
;; https://github.com/swannodette/swannodette.github.com/blob/master/code/blog/src/blog/utils/reactive.cljs
(ns raiseyourgame.lib.ui
  (:refer-clojure :exclude [map filter remove distinct concat take-while])
  (:require [cljs.core.async :refer [>! <! chan put! close! timeout]]
            [jayq.core :as jq :refer [$]]
            [raiseyourgame.lib.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

;; Begin listening on the selected element or elements for events of the
;; specified type or types, putting each one in an output channel. Returns the
;; output channel. The optional function can be used for event preprocessing
;; side effects -- most prominently, calling .preventDefault on events before
;; putting them in the channel.
(defn listen
  ([selector type] (listen selector type nil))
  ([selector type f] (listen selector type f (chan)))
  ([selector type f out]
    (-> ($ selector)
      (jq/on type (fn [e]
                    (when f (f e)) ;; side effect, if desired
                    (put! out e))))
   out))

;; Pass f through to jayq/document-ready
;; TODO: a better way to set up the app
(defn document-ready [f]
  (jq/document-ready f))
