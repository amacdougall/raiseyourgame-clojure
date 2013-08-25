(ns raiseyourgame
  (:require-macros [clang.angular :refer [def.controller defn.scope def.filter fnj]])
  (:require [clojure.string :as cs]
            clang.js-types
            clang.directive.clangRepeat
            goog.net.XhrIo)
  (:use [clang.util :only [? module]]))

;; NOTE: this is equivalent to (.module (.-angular js/window)), kinda
;; ...see clang/util.js
(def main (module "raiseyourgame.web" ["clang"]))
;; could apply .config, etc after this, to do routes

(def.controller main Videos [$scope]
  (. goog.net.XhrIo send
     "http://localhost:3000/videos"
     (fn [event]
       (? event)
       (let [xhr (:target event)
             data (js->clj (.getResponseJSON xhr))]
         (assoc! $scope :videos (atom (map atom data))))))

  (assoc! $scope :videos (atom [(atom {:title "Video 1" :description "foo"})
                                (atom {:title "Video 2" :description "bar"})])))
