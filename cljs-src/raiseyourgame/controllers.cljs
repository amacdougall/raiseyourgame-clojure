(ns raiseyourgame.controllers
  (:require [raiseyourgame.templates :as templates]
            [raiseyourgame.lib.data :as data]
            [cljs.core.async :refer [>! <! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [raiseyourgame.lib.macros :refer [dochan]]))

;; Given an input channel of params maps, returns an output channel containing
;; template-context pairs.
(defn home [in]
  (let [out (chan)]
    (dochan [params in]
      (let [data (<! (data/GET "/api/v1/videos"))]
        (>! out [templates/home {:videos data}])))
    out))

(defn videos [in]
  (let [out (chan)]
    (dochan [params in]
      (let [data (<! (data/GET (str "/api/v1/videos/" (:id params))))]
        (>! out [templates/video {:video data}])))
    out))
