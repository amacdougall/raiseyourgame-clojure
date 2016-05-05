(ns raiseyourgame.handlers-test
  (:require [raiseyourgame.db :refer [initial-state]]
            [raiseyourgame.handlers :as h]
            [com.rpl.specter :as s]
            [cljs.test :refer-macros [deftest is testing run-tests async]]))

;; Demonstrate that initialize handler sets the boot-time empty re-frame app-db
;; atom to db/initial-state, and calls remote/load-current-user.
(deftest test-initialize
  (let [remote-called? (atom false)]
    (with-redefs [raiseyourgame.remote/load-current-user #(reset! remote-called? true)]
      (is (= (h/initialize {}) initial-state))
      (is @remote-called?))))

;; Demonstrate taht current-user-loaded handler sets current user on app db.
(deftest test-current-user-loaded
  (let [user {:username "Alan"}]
    (is (= (h/current-user-loaded initial-state [:current-user-loaded user]) 
           (assoc initial-state :current-user user)))))

(deftest test-update-form-value
  (is (= (h/update-form-value initial-state [:update-form-value :login :username "Test"])
         (assoc-in initial-state [:forms :login :values :username] "Test"))))

(deftest test-update-form-error
  (is (= (h/update-form-error initial-state [:update-form-error :login :bad-data? true])
         (assoc-in initial-state [:forms :login :errors :bad-data?] true))))

;; Demonstrate that login form submit handler extracts login credentials from form and
;; passes them to remote/login.
(deftest test-login-form-submit
  (let [remote-called? (atom false)
        db (s/setval [:forms :login :values]
                     {:username "username", :password "password"}
                     initial-state)]
    (with-redefs [raiseyourgame.remote/login
                  (fn [m]
                    (reset! remote-called? true)
                    (is (= (:username m) "username"))
                    (is (= (:password m) "password")))]
      (is (= (h/login-form-submit db [:login-form-submit]) db))
      (is @remote-called?))))

;; Demonstrate that login success handler clears the login form data from the
;; app db and sets the newly logged-in user.
(deftest test-login-successful
  (let [db (s/setval [:forms :login :values]
                     {:username "username", :password "password"}
                     initial-state)
        user {:username "Alan"}
        result (h/login-successful db [:login-successful user])]
    (is (= (:current-user result) user))
    (is (nil? (get-in result [:forms :login])))))

;; Demonstrate that the :login-error handler dispatches :update-form-error with
;; the correct arguments when given various HTTP error codes.
(deftest test-login-error
  (let [expect-login-error-dispatch
        (fn [expected-k expected-v]
          (fn [[event-type form-id k v]]
            (is (= event-type :update-form-error))
            (is (= form-id :login))
            (is (= k expected-k))
            (is (= v expected-v))))]
    (with-redefs [re-frame.core/dispatch (expect-login-error-dispatch :bad-data? true)]
      (is (= (h/login-error {} [:login-error {:status 400}]) {})))
    (with-redefs [re-frame.core/dispatch (expect-login-error-dispatch :login-failed? true)]
      (is (= (h/login-error {} [:login-error {:status 401}]) {})))
    (with-redefs [re-frame.core/dispatch (expect-login-error-dispatch :system-error? true)]
      (is (= (h/login-error {} [:login-error {:status 500}]) {})))))

;; Demonstrate that the :logout handler invokes remote/logout.
(deftest test-logout
  (let [remote-called? (atom false)
        db {:current-user {:username "Alan"}}]
    (with-redefs [raiseyourgame.remote/logout #(reset! remote-called? true)]
      (is (h/logout db) db)
      (is @remote-called?))))

;; Demonstrate that logout success clears the current user from the app db.
(deftest test-logout-successful
  (let [db {:current-user {:username "Alan"}}]
    (with-redefs [re-frame.core/dispatch
                  (fn [[event-type]]
                    (is (= event-type :display-home)))]
      (is (= (h/logout-successful db) (assoc db :current-user nil))))))

;; Demonstrate that display-home handler sets :target to nil.
(deftest test-display-home
  (is (= (h/display-home (assoc initial-state :target :something))
         initial-state)))

;; Demonstrate that display-user-list handler sets an empty user list target and
;; calls remote/load-users.
(deftest test-display-user-list
  (let [remote-called? (atom false)]
    (with-redefs [raiseyourgame.remote/load-users #(reset! remote-called? true)]
      (is (= (:target (h/display-user-list initial-state))
             {:type :users, :users nil}))
      (is @remote-called?))))

;; Demonstrate that users-loaded handler adds users list to target.
(deftest test-users-loaded
  (let [users [{:username "Terry"} {:username "Andy"}]]
    (is (= (:target (h/users-loaded initial-state [:users-loaded users]))
           {:type :users, :users users}))))
