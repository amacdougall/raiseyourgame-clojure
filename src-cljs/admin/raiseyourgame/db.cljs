(ns raiseyourgame.db)

(def initial-state
  {:current-user nil
   ; data to be viewed or edited in the admin view
   :target nil
   ; values and errors of forms as they are filled, keyed by form id
   :forms {}})
