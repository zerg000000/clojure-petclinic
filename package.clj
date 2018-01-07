#!/usr/bin/env clojure

(require '[clojure.string :as string]
         '[clojure.java.io :as io])

(use '[clojure.java.shell :only [sh]])

; could use tools.deps.alpha and have time to read doc
(def jar-locations (-> (first *command-line-args*)
                       (string/split #":")))

(def target "target/libs/")
(def compile-path "target/classes/")

(println "creating " target)
(io/make-parents target ".ignore")

(doseq [jar jar-locations]
  (let [jar-file (io/file jar)]
    (println "copying " jar)
    (if (.isFile jar-file)
      (sh "tar" "-xf" jar "-C" compile-path))))

(io/make-parents compile-path ".ignore")
(set! *compile-path* compile-path)

(println "compiling petclinic.core")

(compile 'petclinic.core)

(println "packaging uberjar")

(sh "jar" "cvfm" "target/petclinic.jar" "manifest.txt"  "-C" compile-path "." "-C" "resources/" ".")

