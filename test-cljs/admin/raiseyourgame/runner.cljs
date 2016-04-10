(ns raiseyourgame.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [raiseyourgame.core-test]
            [raiseyourgame.handlers-test]
            [raiseyourgame.remote-test]
            [raiseyourgame.subscriptions-test]))

(doo-tests 'raiseyourgame.core-test
           'raiseyourgame.handlers-test
           'raiseyourgame.remote-test
           'raiseyourgame.subscriptions-test)
