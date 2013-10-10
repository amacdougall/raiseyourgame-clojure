(ns raiseyourgame.controllers
  (:require [raiseyourgame.templates :as templates]
            [raiseyourgame.lib.data :as data]
            [cljs.core.async :refer [>! <! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

;; Given an input channel of params maps, returns an output channel containing
;; template-context pairs.
(defn home [in]
  (let [out (chan)]
    (go (loop []
          (if-let [params (<! in)]
            (do
              (>! out [templates/home (<! (data/GET "/api/v1/videos"))])
              (recur))
            (close! in))))
    out))

(defn users [in]
  (let [out (chan)]
    (go (loop []
          (if-let [params (<! in)]
            (do
              ;; DEBUG
              (>! out [templates/home (assoc params :name "users-controller")])
              (recur))
            (close! in))))
    out))
