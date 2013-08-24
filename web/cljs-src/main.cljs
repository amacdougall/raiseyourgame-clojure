(ns raiseyourgame
  (:require-macros [clang.angular :refer [def.controller defn.scope def.filter fnj]])
  (:require [clojure.string :as cs]
            clang.js-types
            clang.directive.clangRepeat)
  (:use [clang.util :only [? module]]))

(def m (module "raiseyourgame.web" ["clang"]))

(def.controller m Videos [$scope]
  (assoc! $scope :characters (atom [(atom {:name "Matthew" :health 100})
                                    (atom {:name "Mark" :health 97})
                                    (atom {:name "Luke" :health 94})
                                    (atom {:name "John" :health 99})])))
