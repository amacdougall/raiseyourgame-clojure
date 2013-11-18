;; Utility functions for DOM interaction and event handling.
;;
;; Heavily based on David Nolen's blog.util.reactive;
;; https://github.com/swannodette/swannodette.github.com/blob/master/code/blog/src/blog/utils/reactive.cljs
(ns raiseyourgame.lib.ui
  (:require [cljs.core.async :refer [>! <! chan put! close! timeout]]
            [enfocus.core :as ef :refer [at]]
            [enfocus.events :as events]
            [enfocus.effects :as effects]
            [raiseyourgame.lib.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [enfocus.macros :as em]))

(set! enfocus.core/debug false)

;; Begin listening on the selected element or elements for events of the
;; specified type or types, putting each one in an output channel. Returns the
;; output channel. The optional function can be used for event preprocessing
;; side effects -- most prominently, calling .preventDefault on events before
;; putting them in the channel. To make this more convenient, the symbol
;; :prevent-default may be provided in place of the side-effect function.
(defn listen
  ([selector type] (listen selector type nil))
  ([selector type f] (listen selector type f (chan)))
  ([selector type f out]
    (at [selector]
      (events/listen type (fn [e]
                            (cond
                              ;; prevent default or perform side effect
                              (= f :prevent-default) (.preventDefault e)
                              (fn? f) (f e))
                            (put! out e))))
   out))

;; Begin listening on the selected element or elements for events of the
;; specified type or types from descendant elements matching the qualifying
;; selector, putting each one in an output channel. Returns the output channel.
;; The optional function can be used for event preprocessing side effects --
;; most prominently, calling .preventDefault on events before putting them in
;; the channel. To make this more convenient, the symbol :prevent-default may be
;; provided in place of the side-effect function.
(defn listen-live
  ([selector type] (listen-live selector type nil))
  ([selector type f] (listen-live selector type f (chan)))
  ([selector type f out]
   (at ["body"]
     (events/listen-live type selector
       (fn [e]
         (cond
           (= f :prevent-default) (.preventDefault e)
           (fn? f) (f e))
         (put! out e))))
   out))

(defn onload [f]
  (set! (.-onload js/window) f))
