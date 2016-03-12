(ns raiseyourgame.core
  (:require [raiseyourgame.views :as views]
            [raiseyourgame.subscriptions]
            [raiseyourgame.handlers]
            [raiseyourgame.routes]
            [reagent.core :as reagent]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [devtools.core :as devtools])
  (:import goog.History))

(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)

(defn main []
  (dispatch-sync [:initialize-db])
  (reagent/render [views/main-view]
                  (.getElementById js/document "app"))

  ; run initial client-side route, if any
  (secretary/dispatch! (.-pathname (.-location js/window)))

  ; handle future navigation events
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (.log js/console "navigation event %o" event)
        ))
    (.setEnabled true)))

(main)
