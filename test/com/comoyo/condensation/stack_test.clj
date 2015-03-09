(ns com.comoyo.condensation.stack-test
  (:require [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [com.comoyo.condensation.stack :refer :all]
            [amazonica.aws.cloudformation :as cf]))

(testable-privates com.comoyo.condensation.stack
                   outputs-vector->keywords-map
                   get-status-from-map)

(fact "outputs-vector->keywords-map"
  (outputs-vector->keywords-map
   [{:description "Public Subnet A"
     :output-key "PublicSubnetA"
     :output-value "subnet-aaaaaaaa"}
    {:description "Public Subnet B"
     :output-key "PublicSubnetB"
     :output-value "subnet-bbbbbbbb"}
    {:description "Public Subnet C"
     :output-key "PublicSubnetC"
     :output-value "subnet-cccccccc"}])
  => {:public-subnet-c "subnet-cccccccc"
      :public-subnet-b "subnet-bbbbbbbb"
      :public-subnet-a "subnet-aaaaaaaa"})

(fact "get-status-from-map"
  (get-status-from-map {:stacks [{:stack-name "network"
                                  :stack-status "UPDATE_COMPLETE"
                                  :outputs [{:description "VPC"
                                             :output-key "Vpc"
                                             :output-value "vpc-abababab"}]}]})
  => :update-complete)

