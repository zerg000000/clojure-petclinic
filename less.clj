#!/usr/bin/env clojure

(require '[clojure.java.io :as io])
(import '[com.github.sommeri.less4j.core DefaultLessCompiler])

(def compiler (DefaultLessCompiler.))

(def css (.compile compiler (io/file "less/petclinic.less")))

(spit "resources/static/resources/css/petclinic.css" (.getCss css))
