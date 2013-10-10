(ns raiseyourgame.lib.data
  (:require [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [>! <! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn GET [url]
  (let [out (chan 1)]
    (xhr/send url
              (fn [event]
                (let [response (-> event .-target .getResponseText)]
                  (go (>! out response)
                      (close! out)))))
    out))
