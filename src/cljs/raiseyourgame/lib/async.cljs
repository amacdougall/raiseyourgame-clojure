;; Heavily based on David Nolen's blog.util.reactive;
;; https://github.com/swannodette/swannodette.github.com/blob/master/code/blog/src/blog/utils/reactive.cljs
;; And since many of these features are getting merged into core.async itself,
;; soon: TODO: use core.async builtin features instead.
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

;; Given a seq of input channels and optionally an output channel, returns an
;; output channel which is given all items from all input channels. When an
;; input channel has a nil value, it is removed from the set of input channels.
;; The origin of the name was not obvious to me:
;; http://en.wikipedia.org/wiki/Fan-in
(defn fan-in
  ([ins] (fan-in ins (chan)))
  ([ins out]
    (go (loop [ins (vec ins)]
          (when (> (count ins) 0)
            (let [[x in] (alts! ins)]
              (when x
                (>! out x)
                (recur ins))
              (recur (vec (disj (set ins) in))))))
        (close! out))
    out))

;; TODO: This has all channels and subscribers in the whole app, so it might
;; need some extra bookkeeping functionality (or the ability to push
;; channel/subscriber storage off onto clients).
(let [channels (atom {})
      subscribers (atom {})]
  (defn publish [k v]
    (when-not (@channels k)
      (swap! channels assoc k (chan)))
    (when-not (@subscribers k)
      (swap! subscribers assoc k (atom [])))
    (doseq [c @(@subscribers k)]
      (put! c v)))

  (defn subscribe [k]
    (when-not (k @subscribers)
      (swap! subscribers assoc k (atom [])))
    (let [k-subscribers (@subscribers k)
          out (chan)]
      (swap! k-subscribers conj out)
      out)))
