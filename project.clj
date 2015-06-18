(defproject com.comoyo/condensation "0.2.3-SNAPSHOT"
  :description "A Clojure library for making AWS CloudFormation easier to use."
  :url "https://github.com/comoyo/condensation"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.5"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [amazonica "0.3.18"]]
  :plugins [[midje-readme "1.0.7"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :midje-readme {:require "[com.comoyo.condensation.template :as cft]"})
