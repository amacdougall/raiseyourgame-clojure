(ns raiseyourgame.controllers
  (:require [raiseyourgame.templates :as templates]
            [raiseyourgame.timeline :as timeline]
            [raiseyourgame.lib.data :as data]
            [cljs.core.async :refer [>! <! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [raiseyourgame.lib.macros :refer [dochan]]))

(def dummy-script [{:time 1 :text "One second. Are you excited yet?"}
                   {:time 3 :text "Three seconds. Nothing much is happening actually."}
                   {:time 5 :text "Five seconds. But now it's AWESOME!"}
                   {:time 7 :text "Seven seconds. ...not really."}
                   {:time 10 :text "Ten seconds."}
                   {:time 15 :text "Fifteen seconds."}
                   {:time 20 :text "Twenty seconds. Yay!"}
                   {:time 30 :text "Thirty seconds."}])

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
        (timeline/run dummy-script)
        (>! out [templates/video {:video data}])))
    out))
