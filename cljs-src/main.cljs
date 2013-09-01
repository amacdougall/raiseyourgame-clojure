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

(defn load-list [$scope uri key]
  (xhr [:get uri]
       nil ;; no data sent
       (fn [data]
         (let [items (-> data JSON/parse js->clj)
               item-atoms (vec (map atom items))
               list-atom (atom item-atoms)]
           (.$apply $scope #(assoc! $scope key list-atom))))))

(def.controller main Videos [$scope]
  (load-list $scope "/api/v1/videos" :videos)

  ;; provide initial data
  (assoc! $scope :videos (atom [(atom {:title "Video 1" :description "foo"})
                                (atom {:title "Video 2" :description "bar"})])))
