(ns raiseyourgame.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [raiseyourgame.core-test]))

(doo-tests 'raiseyourgame.core-test)
