(ns raiseyourgame.lib.data
  (:require [goog.net.XhrIo :as xhr]
            [cljs.core.async :refer [>! <! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def media-types
  {:application-json "application/json"
   :application-edn "application/edn"})

(defn GET
  ([url]
   (GET url :application-json))
  ([url media-type]
   (let [out (chan 1)]
     (xhr/send url
               (fn [event]
                 (let [response (-> event .-target .getResponseText)]
                   (go (>! out response)
                       (close! out))))
               "GET"
               nil ; no extra content
               (clj->js {"Accept" (media-type media-types)})) ; headers
     out)))
