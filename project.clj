(defproject bestcase-examples "0.1.0"
  :description "Usage examples of the bestcase a/b testing and multivariate testing library."
  :url "https://github.com/jeandenis/bestcase-examples"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [bestcase "0.1.0"]
                 [compojure "1.1.3"]
                 [ring "1.1.6"]
                 [hiccup "1.0.1"]]
  :plugins [[lein-ring "0.7.1"]]
  :ring {:handler bestcase.examples.three-page-app.core/app})
                

