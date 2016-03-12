(ns raiseyourgame.core
  (:require [raiseyourgame.views :as views]
            [raiseyourgame.subscriptions]
            [raiseyourgame.handlers]
            [raiseyourgame.routes]
            [reagent.core :as reagent]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [devtools.core :as devtools]))

(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)

(defn main []
  (dispatch-sync [:initialize-db])
  (reagent/render [views/main-view]
                  (.getElementById js/document "app")))

(main)
