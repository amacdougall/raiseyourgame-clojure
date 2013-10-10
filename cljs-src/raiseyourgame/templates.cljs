(ns raiseyourgame.templates)

(defn home [context]
  (.log js/console "(templates/home %o)" (clj->js context)))
