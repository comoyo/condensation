(ns com.comoyo.condensation.stack
  (:require [clojure.data.json :as json]
            [clojure.set :as set]
            [amazonica.aws.cloudformation :as cf]
            [org.tobereplaced.lettercase :as lc])
  (:import [com.amazonaws.AmazonServiceException]))

(def ^:const ^:private default-timeout-ms (* 10 60 1000))

(def ^:const ^:private status-successfully-complete #{:create-complete
                                                      :update-complete})

(def ^:const ^:private status-successfully-rolled-back #{:rollback-complete
                                                         :update-rollback-complete})

(def ^:const ^:private status-in-progress #{:create-in-progress
                                            :delete-in-progress
                                            :rollback-in-progress
                                            :update-complete-cleanup-in-progress
                                            :update-in-progress
                                            :update-rollback-complete-cleanup-in-progress
                                            :update-rollback-in-progress})

(def ^:const ^:private status-ready (set/union status-successfully-complete
                                               status-successfully-rolled-back))

(defn- outputs-vector->keywords-map
  [outputs]
  {:pre [(vector? outputs)]}
  (reduce #(assoc %1
                  (keyword (lc/lower-hyphen (:output-key %2)))
                  (:output-value %2))
          {}
          outputs))

(defn get-outputs
  "Get the Outputs for a stack in a keyword map."
  [stack-name]
  (outputs-vector->keywords-map
   (:outputs
    (first
     (:stacks
      (cf/describe-stacks :stack-name stack-name))))))

(defn- get-status-from-map
  [stacks-map]
  {:pre [(= 1 (count (:stacks stacks-map)))]
   :post [(keyword? %)]}
  (lc/lower-hyphen-keyword
   (:stack-status
    (first
     (:stacks stacks-map)))))

(defn get-stack-status
  "Get the status of a stack as a keyword."
  [stack-name]
  (try
    (get-status-from-map
     (cf/describe-stacks :stack-name stack-name))
    (catch com.amazonaws.AmazonServiceException _)))

(defn stack-exists?
  "Check if a stack exists."
  [stack-name]
  (not (nil? (get-stack-status stack-name))))

(defn stack-ready?
  "Check if stack is ready, that is, not being updated or in an invalid state."
  [stack-name]
  (contains? status-ready (get-stack-status stack-name)))

(defn stack-successfully-rolled-back?
  "Check if a stack has been successfully rolled back."
  [stack-name]
  (contains? status-successfully-rolled-back (get-stack-status stack-name)))

(defn stack-in-progress?
  "Check if a stack is being updated."
  [stack-name]
  (contains? status-in-progress (get-stack-status stack-name)))

(defn- wait-for-stack-ms
  [stack-name
   timeout-ms]
  (let [future-stack (future
                       (loop []
                         (if (stack-ready? stack-name)
                           (not (stack-successfully-rolled-back? stack-name))
                           (when (stack-in-progress? stack-name)
                             (do
                               (Thread/sleep 10000)
                               (recur))))))]
    (deref future-stack timeout-ms nil)))

(defn- wait-for-stack-deleted-ms
  [stack-name
   timeout-ms]
  (let [future-stack (future
                       (loop []
                         (if (stack-in-progress? stack-name)
                           (do
                             (Thread/sleep 10000)
                             (recur))
                           (not (stack-exists? stack-name)))))]
    (deref future-stack timeout-ms nil)))

(defn- create
  [stack-name
   template
   capabilities]
  (cf/create-stack :stack-name stack-name
                   :template-body (json/write-str template)
                   :capabilities capabilities))

(defn- update
  [stack-name
   template
   capabilities]
  (cf/update-stack :stack-name stack-name
                   :template-body (json/write-str template)
                   :capabilities capabilities))

(defn- create-or-update
  [stack-name
   template
   capabilities]
  (if (stack-exists? stack-name)
    (if (stack-ready? stack-name)
      (update stack-name template capabilities)
      (throw (RuntimeException. (str "Stack " stack-name " in invalid state."))))
    (create stack-name template capabilities)))

(defn create-or-update-stack
  "Create a stack if it does not exist, update if it does."
  [&
   {:keys [stack-name
           template
           capabilities
           wait?
           wait-timeout-ms]
    :or {capabilities ["CAPABILITY_IAM"]
         wait? true
         wait-timeout-ms default-timeout-ms}}]
  {:pre [(string? stack-name)
         (map? template)]}
  (let [result-map (create-or-update stack-name template capabilities)]
    (if wait?
      (wait-for-stack-ms stack-name wait-timeout-ms)
      result-map)))

(defn delete-stack
  "Delete a stack."
  [&
   {:keys [stack-name
           wait?
           wait-timeout-ms]
    :or {wait? true
         wait-timeout-ms default-timeout-ms}}]
  {:pre [(string? stack-name)]}
  (let [result-map (cf/delete-stack :stack-name stack-name)]
    (if wait?
      (wait-for-stack-deleted-ms stack-name wait-timeout-ms)
      result-map)))

