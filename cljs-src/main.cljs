(ns raiseyourgame
  (:require-macros [clang.angular :refer [def.controller defn.scope def.filter fnj]])
  (:require [clojure.browser.repl]
            clang.js-types
            clang.directive.clangRepeat
            [clang.util :refer [? module]]
            [fetch.core :refer [xhr]]))

(def main (module "raiseyourgame" ["clang" "ngRoute"]))

(defn routes [$routeProvider, $locationProvider]
  (.html5Mode $locationProvider true)
  (-> $routeProvider
    (.when "/videos" (clj->js {:templateUrl "static/partials/videoList.html"
                               :controller "VideoListController"}))
    (.when "/video/:id" (clj->js {:templateUrl "static/partials/video.html"
                                  :controller "VideoController"}))
    (.otherwise (clj->js {:redirectTo "/videos"}))))

(.config main routes)

(defn load-list [$scope uri key]
  (xhr [:get uri]
       nil ;; no data sent
       (fn [data]
         (let [items (-> data JSON/parse js->clj)
               item-atoms (vec (map atom items))
               list-atom (atom item-atoms)]
           (.$apply $scope #(assoc! $scope key list-atom))))))

(def.controller main VideoListController [$scope]
  (defn.scope update-scope [k v]
    (assoc! $scope k (atom v)))

  (load-list $scope "/api/v1/videos" :videos)

  ;; provide initial data
  (assoc! $scope :videos (atom [(atom {:title "Video 1" :description "foo"})
                                (atom {:title "Video 2" :description "bar"})])))
