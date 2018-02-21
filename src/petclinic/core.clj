(ns petclinic.core
  (:require [bidi.bidi :as bidi]
            [petclinic.thymeleaf :as thymeleaf]
            [aleph.http :as http]
            [ring.middleware.webjars :as webjars]
            [ring.middleware.resource :as resource]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.params :as params]
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
    (content-type/wrap-content-type)
    (wrap-bidi c/routes)))

(defn -main [& args]
  (let [mode (or (first args) "dev")
        real-app (if (= "prod" mode)
                   app
                   (do
                     (use '[ring.middleware.reload])
                     (use '[ring.middleware.stacktrace])
                     (-> #'app
                       ((resolve 'wrap-reload) '[petclinic.core])
                       ((resolve 'wrap-stacktrace)))))]  
    (println "production mode: " mode)
    (http/start-server real-app {:port 3000})))
