(ns raiseyourgame.test.routes-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [raiseyourgame.handler :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response))))))

; This is actually a test test, delete it later
(deftest test-plus
  (testing "basic addition"
    (let [response (app (request :get "/api/plus" {:x 1 :y 1}))]
      (is (= 200 (:status response)))
      (is (= 2 (read-string (slurp (:body response))))))))
