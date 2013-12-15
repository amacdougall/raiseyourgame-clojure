(ns raiseyourgame.lib.history)

;; Takes function expecting a history state; invokes that function whenever a
;; history navigation occurs.
(defn bind-navigation-handler [f]
  (let [history (.-History js/window)
        adapter (.-Adapter history)
        state-change-handler (fn [] (f (.getState history)))]
    (.bind adapter js/window "statechange" state-change-handler)))

(defn push-state [pathname]
  (.pushState (.-History js/window) nil nil pathname))

(defn state->route [state]
  (.-hash state))
