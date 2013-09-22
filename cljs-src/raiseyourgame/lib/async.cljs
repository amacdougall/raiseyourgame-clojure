;; Heavily based on David Nolen's blog.util.reactive;
;; https://github.com/swannodette/swannodette.github.com/blob/master/code/blog/src/blog/utils/reactive.cljs
(ns raiseyourgame.lib.async
  (:refer-clojure :exclude [map filter remove distinct concat take-while])
  (:require [cljs.core.async :refer [>! <! chan put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [raiseyourgame.lib.macros :refer [dochan]]))

;; Pass each value through to the output channel, logging it out along the way.
(defn log [in]
  (let [out (chan)]
    (dochan [e in]
      (.log js/console e)
      (>! out e))
    out))

;; Given a function f and an input channel... sets up a go block consisting of
;; a loop which takes elements from the input channel, applies f to them, and
;; puts the result in the output channel. Returns the output channel. When the
;; input channel is empty, the output channel is closed.
(defn map [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (>! out (f x))
              (recur))
            (close! out))))
    out))

;; Given a value and an input channel, takes values from the input channel, but
;; puts the value in the output channel. A channel version of constantly.
(defn always [v c]
  (let [out (chan)]
    (go (loop []
          (if-let [e (<! c)]
            (do (>! out v)
              (recur))
            (close! out))))
    out))
