(ns com.comoyo.condensation.template
  (:refer-clojure :exclude [ref])
  (:require [clojure.string :as string]
            [org.tobereplaced.lettercase :as lc]))

(defn- symbol->logical-name
  [symbol]
  (lc/capitalized-name symbol))

(defn make-resource
  "Create a resource value."
  [name
   specification-function]
  {:name (symbol->logical-name name)
   :specification-function specification-function})

(defmacro defresource
  "Create a resource and bind to var."
  ([name
    specification]
   `(defresource ~name [] ~specification))
  ([name
    parameters
    specification]
   `(def ~name (make-resource '~name (fn ~parameters ~specification)))))

(defn ref
  "Create a CF ref map."
  [resource]
  {"Ref" (:name resource)})

(defn get-att
  "Create a CF Fn::GetAtt map."
  [resource
   attribute-name]
  {"Fn::GetAtt" [(:name resource) attribute-name]})

(defn get-parameters-resource-map
  "Get a resource map for a resource with parameters."
  [resource
   parameters]
  {(:name resource) (apply (:specification-function resource) parameters)})

(defn get-no-parameters-resource-map
  "Get a resource map for a resource with no parameters."
  [resource]
  {(:name resource) ((:specification-function resource))})

(defn- get-resource-map
  [resource]
  {:pre [(or (symbol? resource)
             (and (seq? resource)
                  (symbol? (first resource))))]}
  (if (list? resource)
    `(get-parameters-resource-map ~(first resource) ~(into [] (rest resource)))
    `(get-no-parameters-resource-map ~resource)))

(defmacro resources [& resources]
  "Create a resource map for a set of resources."
  `(into {}
         [~@(map get-resource-map resources)]))

(defn make-output
  "Create an Outputs map."
  [name
   description
   value]
  {:name (symbol->logical-name name)
   :description description
   :value value})

(defmacro defoutput
  "Create an Outputs map and bind to var."
  [name
   description
   value]
  `(def ~name (make-output ~(symbol->logical-name name)
                           ~description
                           ~value)))

(defn outputs
  "Create an Ouputs map for a sequence of output values."
  [& outputs]
  (into {}
        (map (fn [output] [(:name output) {"Description" (:description output)
                                           "Value" (:value output)}]) outputs)))

(defn template
  "Create a complete CF template."
  [& {:keys [description
             resources
             outputs]
      :or {outputs nil}}]
  {:pre [(string? description)
         (not (string/blank? description))
         (map? resources)
         (or (nil? outputs) (map? outputs))]}
  (merge {"AWSTemplateFormatVersion" "2010-09-09"
          "Description" description
          "Resources" resources}
         (when outputs
           {"Outputs" outputs})))

