(ns raiseyourgame.remote-test
  (:require [raiseyourgame.db :refer [initial-state]]
            [raiseyourgame.remote :as r]
            [com.rpl.specter :as s]
            [cljs.test :refer-macros [deftest is testing run-tests async]]))

;; Demonstrate that remote/load-current-user calls GET with the correct API and
;; options.
(deftest test-load-current-user
  ; test success
  (let [dispatched? (atom false)
        mock-user {:username "Ryo"}]
    (with-redefs
      [ajax.core/GET
       (fn [uri {handler :handler}]
         (is (= uri "/api/users/current"))
         (handler mock-user))
       re-frame.core/dispatch
       (fn [[event-id user]]
         (is (= event-id :current-user-loaded))
         (is (= user mock-user))
         (reset! dispatched? true))]
      (r/load-current-user)
      (is @dispatched?)))

  ; test failure
  (let [dispatched? (atom false)]
    (with-redefs
      [ajax.core/GET
       (fn [uri {error-handler :error-handler}]
         (is (= uri "/api/users/current"))
         (error-handler {:status 404})) ; call with mock response
       re-frame.core/dispatch
       (fn [[event-id user]]
         (is (= event-id :current-user-loaded))
         (is (= user nil))
         (reset! dispatched? true))]
      (r/load-current-user)
      (is @dispatched?))))

(deftest test-login
  ; test success
  (let [dispatched? (atom false)
        mock-user {:username "Ryo"}
        credentials {:username "Ryo" :password "password"}]
    (with-redefs
      [ajax.core/POST
       (fn [uri {params :params handler :handler}]
         (is (= uri "/api/users/login"))
         (is (= params credentials))
         (handler mock-user))
       re-frame.core/dispatch
       (fn [[event-id user]]
         (is (= event-id :login-successful))
         (is (= user mock-user))
         (reset! dispatched? true))]
      (r/login credentials)
      (is @dispatched?)))

  ; test failure
  (let [dispatched? (atom false)]
    (with-redefs
      [ajax.core/POST
       (fn [uri {error-handler :error-handler}]
         (is (= uri "/api/users/login"))
         (error-handler {:status 401})) ; call with mock response
       re-frame.core/dispatch
       (fn [[event-id error]]
         (is (= event-id :login-error))
         (is (not (nil? error)))
         (reset! dispatched? true))]
      (r/login {:username "Ryo" :password "password"})
      (is @dispatched?))))

;; Demonstrate that logout dispatches logout-success or logout-failure.
(deftest test-logout
  ;; test success
  (let [dispatched? (atom false)]
    (with-redefs
      [ajax.core/POST
       (fn [uri {handler :handler}]
         (is (= uri "/api/users/logout"))
         (handler {:status 204})) ; call with mock response
       re-frame.core/dispatch
       (fn [[event-id]]
         (is (= event-id :logout-successful))
         (reset! dispatched? true))]
      (r/logout)
      (is @dispatched?)))
  
  ;; test failure
  (let [dispatched? (atom false)]
    (with-redefs
      [ajax.core/POST
       (fn [uri {error-handler :error-handler}]
         (is (= uri "/api/users/logout"))
         (error-handler {:status 404})) ; call with mock response
       re-frame.core/dispatch
       (fn [[event-id]]
         (is (= event-id :logout-failed))
         (reset! dispatched? true))]
      (r/logout)
      (is @dispatched?)))
  )

(deftest test-load-users
  (let [dispatched? (atom false)
        mock-users-response {:page 1, :per-page 30
                             :users [{:username "Ryo"} {:username "Yuri"}]}
        mock-users (:users mock-users-response)]
    (with-redefs
      [ajax.core/GET
       (fn [uri {handler :handler}]
         (is (= uri "/api/users"))
         (handler mock-users-response))
       re-frame.core/dispatch
       (fn [[event-id users]]
         (is (= event-id :users-loaded))
         (is (= users mock-users))
         (reset! dispatched? true))]
      (r/load-users)
      (is @dispatched?))))
