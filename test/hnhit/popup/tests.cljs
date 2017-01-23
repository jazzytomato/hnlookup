(ns hnhit.popup.tests
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]))

;; TODO configure a runner with figwheel
(deftest test-numbers
  (is (= 1 1)))

(enable-console-print!)
(cljs.test/run-tests)