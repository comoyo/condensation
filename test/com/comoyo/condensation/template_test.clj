(ns com.comoyo.condensation.template-test
  (:refer-clojure :exclude [ref])
  (:require [midje.sweet :refer :all]
            [com.comoyo.condensation.template :refer :all]))

(fact "make-resource"
  (let [specification-function (fn [x] {"XYZ" x})]
    (make-resource 'some-name specification-function)
    => {:name "SomeName"
        :specification-function specification-function}))

(defresource gateway
  {"Type" "AWS::EC2::InternetGateway"})

(defresource vpc
  {"Type" "AWS::EC2::VPC"
   "Description" "Analytics VPC"
   "Properties" {"CidrBlock" "10.10.0.0/16"
                 "EnableDnsSupport" "true"
                 "EnableDnsHostnames" "true"}})

(defresource route-table
  {"Type" "AWS::EC2::RouteTable"
   "Properties" {"VpcId" (ref vpc)}})

(defresource security-group
  [vpc]
  {"Type" "AWS::EC2::SecurityGroup"
   "Properties" {"GroupDescription" "Jump security group"
                 "VpcId" vpc
                 "SecurityGroupIngress" [{"IpProtocol" "tcp"
                                          "FromPort" "22"
                                          "ToPort" "22"
                                          "CidrIp" "0.0.0.0/0"}]}})

(facts "ref"
  (fact "gateway"
    (ref gateway) => {"Ref" "Gateway"})
  (fact "route-table"
    (ref route-table) => {"Ref" "RouteTable"})
  (fact "indirect"
    (let [some-resource vpc]
      (ref some-resource) => {"Ref" "Vpc"}))
  (fact "parameter resource"
    (ref security-group) => {"Ref" "SecurityGroup"}))

(fact "get-att"
  (get-att vpc "DefaultSecurityGroup")
  => {"Fn::GetAtt" ["Vpc" "DefaultSecurityGroup"]})

(fact "get-parameters-resource-map"
  (get-parameters-resource-map security-group ["vpc-id"])
  => {"SecurityGroup" {"Properties" {"GroupDescription" "Jump security group"
                                     "SecurityGroupIngress" [{"CidrIp" "0.0.0.0/0"
                                                              "FromPort" "22"
                                                              "IpProtocol" "tcp"
                                                              "ToPort" "22"}]
                                     "VpcId" "vpc-id"}
                       "Type" "AWS::EC2::SecurityGroup"}})

(fact "get-no-parameters-resource-map"
  (get-no-parameters-resource-map gateway)
  => {"Gateway" {"Type" "AWS::EC2::InternetGateway"}})

(def test-resources
  (resources gateway
             vpc
             (security-group "vpc-id")
             route-table))

(def test-resources-map
  {"Gateway" {"Type" "AWS::EC2::InternetGateway"}
   "RouteTable" {"Properties" {"VpcId" {"Ref" "Vpc"}}
                 "Type" "AWS::EC2::RouteTable"}
   "SecurityGroup" {"Properties" {"GroupDescription" "Jump security group"
                                  "SecurityGroupIngress" [{"CidrIp" "0.0.0.0/0"
                                                           "FromPort" "22"
                                                           "IpProtocol" "tcp"
                                                           "ToPort" "22"}]
                                  "VpcId" "vpc-id"}
                    "Type" "AWS::EC2::SecurityGroup"}
   "Vpc" {"Description" "Analytics VPC"
          "Properties" {"CidrBlock" "10.10.0.0/16"
                        "EnableDnsHostnames" "true"
                        "EnableDnsSupport" "true"}
          "Type" "AWS::EC2::VPC"}})

(fact "resources"
  test-resources => test-resources)

(fact "make-output"
  (make-output 'abc-def "Some output" 7)
  => {:name "AbcDef" :description "Some output" :value 7})

(defoutput vpc-security-group
  "The VPC default security group"
  (get-att vpc "DefaultSecurityGroup"))

(defoutput security-group-id
  "The security group ID"
  (ref security-group))

(def test-outputs
  (outputs vpc-security-group
           security-group-id))

(def test-outputs-map
  {"SecurityGroupId" {"Description" "The security group ID"
                      "Value" {"Ref" "SecurityGroup"}}
   "VpcSecurityGroup" {"Description" "The VPC default security group"
                       "Value" {"Fn::GetAtt" ["Vpc" "DefaultSecurityGroup"]}}})

(fact "outputs"
  test-outputs => test-outputs-map)

(facts "template"
  (fact "without outputs"
    (template :description "A template"
              :resources test-resources)
    => {"AWSTemplateFormatVersion" "2010-09-09"
        "Description" "A template"
        "Resources" test-resources-map})
  (fact "with outputs"
    (template :description "Another template"
              :resources test-resources
              :outputs test-outputs)
    => {"AWSTemplateFormatVersion" "2010-09-09"
        "Description" "Another template"
        "Resources" test-resources-map
        "Outputs" test-outputs-map}))


