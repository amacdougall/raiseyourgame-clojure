(ns raiseyourgame
  (:require-macros [clang.angular :refer [def.controller defn.scope def.filter fnj]])
  (:require [clojure.browser.repl]
            clang.js-types
            clang.directive.clangRepeat
            [clang.util :refer [? module]]
            [fetch.core :refer [xhr]]))

;; NOTE: this is equivalent to (.module (.-angular js/window)), kinda
;; ...see clang/util.js
(def main (module "raiseyourgame.web" ["clang"]))
;; could apply .config, etc after this, to do routes

(def.controller main Videos [$scope]
  (xhr [:get "/api/v1/videos"]
       nil
       (fn [data]
         (let [items (-> data JSON/parse js->clj)
               item-atoms (vec (map atom items))
               videos-atom (atom item-atoms)]
           ;; TODO: make prettier, but this totally works!
           (.$apply $scope
                    (fn []
                      (assoc! $scope :videos videos-atom))))))

  (assoc! $scope :videos (atom [(atom {:title "Video 1" :description "foo"})
                                (atom {:title "Video 2" :description "bar"})])))
