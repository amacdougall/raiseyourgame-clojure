(ns raiseyourgame.core
  (:require [raiseyourgame.views :as views]
            [raiseyourgame.subscriptions]
            [raiseyourgame.handlers]
            [reagent.core :as reagent]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [devtools.core :as devtools]))

(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)

(defn main []
  (dispatch-sync [:initialize-db])
  (dispatch [:display-user-list])
  (reagent/render [views/users-view]
                  (.getElementById js/document "app")))

(main)
