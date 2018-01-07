(ns petclinic.core
  (:require [bidi.bidi :as bidi]
            [petclinic.thymeleaf :as thymeleaf]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.webjars :as webjars]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [ring.middleware.reload :as reload]
            [ring.middleware.stacktrace :as stacktrace]
            [petclinic.controller :as c])
  (:gen-class))

(defn wrap-bidi [handler routes]
  (fn [{:keys [uri path-info] :as req}]
    (handler (merge req (bidi/match-route* routes (or uri path-info) req)))))

(defn dispatcher [{:keys [handler]
                   :as req}]
  (if handler
    (handler req)
    (c/server-error-page req))) 

(def app
  (-> #'dispatcher
    (thymeleaf/wrap-thymeleaf)
    (resource/wrap-resource "static")
    (webjars/wrap-webjars "/webjars")
    (params/wrap-params)
    (wrap-bidi c/routes)))

(defn -main [& args]
  (let [mode (or (first args) "dev")
        real-app (if (= "prod" mode)
                   app
                   (-> #'app
                     (reload/wrap-reload '[petclinic.core])
                     (stacktrace/wrap-stacktrace)))]  
    (println "production mode: " mode)
    (jetty/run-jetty real-app {:port 3000})))
