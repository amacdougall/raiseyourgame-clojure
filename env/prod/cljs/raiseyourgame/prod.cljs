(ns raiseyourgame.app
  (:require [raiseyourgame.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
