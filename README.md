[![travis-ci.org](https://travis-ci.org/comoyo/condensation.svg?branch=master)](https://travis-ci.org/comoyo/condensation)

# Condensation

A Clojure library for making AWS CloudFormation easier to use.

## Usage

Leiningen coordinate:

```clj
[com.comoyo/condensation "0.2.1"]
```

## Template definition

To define templates, require
`[com.comoyo.condensation.template :as cft]`.

Then, define resources:

```clojure
(cft/defresource vpc
  {"Type" "AWS::EC2::VPC"
   "Properties" {"CidrBlock" "10.10.0.0/16"
                 "EnableDnsSupport" "true"
                 "EnableDnsHostnames" "true"}})

(cft/defresource route-table
  {"Type" "AWS::EC2::RouteTable"
   "Properties" {"VpcId" (cft/ref vpc)}})
```

We can also create resources that take parameters:

```clojure
(cft/defresource security-group
  [vpc]
  {"Type" "AWS::EC2::SecurityGroup"
   "Properties" {"VpcId" vpc
                 "SecurityGroupIngress" [{"IpProtocol" "tcp"
                                          "FromPort" "22"
                                          "ToPort" "22"
                                          "CidrIp" "0.0.0.0/0"}]}})
```

Logical names are automatically defined based on resource names.

We can now use the `ref` function to refer to resources, like with

```clojure
(cft/ref vpc) => {"Ref" "Vpc"}
```

above.

The `get-att` function can be used to refer to attributes:

```clojure
(cft/get-att vpc "DefaultSecurityGroup")
=> {"Fn::GetAtt" ["Vpc" "DefaultSecurityGroup"]}
```

We can now use the `resources` macro to access the Resources map:

```clojure
(cft/resources vpc
               route-table
               (security-group "vpc-id"))
=> {"RouteTable" {"Properties" {"VpcId" {"Ref" "Vpc"}}
                  "Type" "AWS::EC2::RouteTable"}
    "Vpc" {"Properties" {"CidrBlock" "10.10.0.0/16"
                         "EnableDnsHostnames" "true"
                         "EnableDnsSupport" "true"}
           "Type" "AWS::EC2::VPC"}
    "SecurityGroup" {"Type" "AWS::EC2::SecurityGroup"
                     "Properties" {"VpcId" "vpc-id"
                                   "SecurityGroupIngress" [{"IpProtocol" "tcp"
                                                            "FromPort" "22"
                                                            "ToPort" "22"
                                                            "CidrIp" "0.0.0.0/0"}]}}}
```

We can also define outputs using the `defoutputs` macro:

```clojure
(cft/defoutput vpc-id
  "The VPC ID"
  (cft/ref vpc))

(cft/defoutput security-group-id
  "The security group ID"
  (cft/ref security-group))
```

The `outputs` function then creates the outputs map:

```clojure
(cft/outputs vpc-id
             security-group-id)
=> {"SecurityGroupId" {"Description" "The security group ID"
                       "Value" {"Ref" "SecurityGroup"}}
    "VpcId" {"Description" "The VPC ID"
             "Value" {"Ref" "Vpc"}}}
```
Finally, the `templates` function can be used to create the template
map:

```clojure
(cft/template :description "A template"
              :resources (cft/resources vpc
                                        route-table
                                        (security-group "vpc-id"))
              :outputs (cft/outputs vpc-id
                                    security-group-id))
=> {"AWSTemplateFormatVersion" "2010-09-09"
    "Description" "A template"
    "Outputs" {"SecurityGroupId" {"Description" "The security group ID"
                                  "Value" {"Ref" "SecurityGroup"}}
               "VpcId" {"Description" "The VPC ID"
                        "Value" {"Ref" "Vpc"}}}
    "Resources" {"RouteTable" {"Properties" {"VpcId" {"Ref" "Vpc"}}
                               "Type" "AWS::EC2::RouteTable"}
                 "Vpc" {"Properties" {"CidrBlock" "10.10.0.0/16"
                                      "EnableDnsHostnames" "true"
                                      "EnableDnsSupport" "true"}
                        "Type" "AWS::EC2::VPC"}
                 "SecurityGroup" {"Type" "AWS::EC2::SecurityGroup"
                                  "Properties" {"VpcId" "vpc-id"
                                                "SecurityGroupIngress" [{"IpProtocol" "tcp"
                                                                         "FromPort" "22"
                                                                         "ToPort" "22"
                                                                         "CidrIp" "0.0.0.0/0"}]}}}}
```

## Dealing with stacks

To create a stack, use the `create-or-update-stack` function:

```clj
(create-or-update-stack :stack-name "dummy" :cloudformation-template-map template)
```

where `template` is created by `template/template` as described above.

If the stack already exists it is updated.

Use `:wait? false` to return without waiting for the create/update
operation to be done.

To delete a stack, use `delete-stack`:

```clj
(delete-stack :stack-name "dummy")
```

