(ns petclinic.thymeleaf
  (:require [clojure.walk :as walk]
            [clojure.java.io :as io])
  (:import [org.thymeleaf.templateresolver ClassLoaderTemplateResolver]
           [org.thymeleaf.messageresolver StandardMessageResolver]
           [org.thymeleaf TemplateEngine]
           [org.thymeleaf.context Context]
           [org.thymeleaf.linkbuilder ILinkBuilder]
           [java.util Locale Properties]))

(def engine (atom nil))

(defn init-thymeleaf []
  (let [resolver (ClassLoaderTemplateResolver.)
        message-resolver (StandardMessageResolver.)
        messages (Properties.)
        new-engine (TemplateEngine.)
        link-builder (reify ILinkBuilder
                        (getName [this] "RingLinkBuilder")
                        (getOrder [this] (int 1))
                        (buildLink [this ctx base params]
                          base))]
    (.load messages (io/input-stream (io/resource "messages/messages.properties")))
    (.setDefaultMessages message-resolver messages)
    (.setPrefix resolver "templates/")
    (.setSuffix resolver ".html")
    (.setCacheable resolver false)
    (.setTemplateResolver new-engine resolver)
    (.setMessageResolver new-engine message-resolver)
    (.setLinkBuilder new-engine link-builder)
    (reset! engine new-engine)))

(defn data->context [data]
  (Context. 
    (Locale/getDefault)
    (walk/stringify-keys data)))

(defn render [[view data]]
  (if-not @engine
    (init-thymeleaf))
  (.process @engine view (data->context data)))

(defn wrap-thymeleaf [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (vector? (:body resp))
        (update resp :body render)
        resp))))
        