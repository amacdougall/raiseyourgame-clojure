(ns raiseyourgame.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [raiseyourgame.core-test]
            [raiseyourgame.handlers-test]))

(doo-tests 'raiseyourgame.core-test
           'raiseyourgame.handlers-test)
