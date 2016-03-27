(ns raiseyourgame.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]))

(deftest test-numbers
  (is (= 1 1))
  (is (= 2 (+ 1 1))))
