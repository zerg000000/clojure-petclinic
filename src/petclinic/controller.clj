(ns petclinic.controller
  (:require 
    [petclinic.db :as db]  
    [petclinic.specs :as specs]
    [clojure.spec.alpha :as s]
    [clojure.walk :as walk]
    [ring.util.response :as response]
    [bidi.bidi :as bidi]
    [cheshire.core :as json]
    [clojure.xml :as xml]
    [clojure.pprint :as pp]))

(declare routes)

(defn str->int [s]
  (try
    (Integer/parseInt s)
    (catch Exception ex
      0)))

(defn page [view data]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body [view data]})

(defn json [data]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/encode data)})

(defn vets->xml [vets]
  {:tag :vets
   :content (map 
              (fn [vet]
                {:tag :vet
                 :attrs (dissoc vet :specialties)
                 :content (map
                            (fn [sp]
                              {:tag :specialty :attrs sp})
                            (:specialties vet))})
              vets)})

(defn vets-xml [data]
  {:status 200
   :headers {"Content-Type" "application/xml"}
   :body (with-out-str (xml/emit-element (vets->xml data)))})

(defn process-if-valid [spec form success-fn fail-fn]
  (let [clojure-form (walk/keywordize-keys form)
        errors (s/explain-data spec clojure-form)]
    (if errors
      (fail-fn clojure-form (specs/error-message clojure-form errors))
      (success-fn (s/conform spec clojure-form)))))

(defn welcome [_]
  (page "welcome" {:welcome "welcome"}))

(defn vets [{{:keys [export-format]} :route-params}]
  (condp = export-format
    "json" (json (db/get-vets))
    "xml" (vets-xml (db/get-vets))
    (page "vets/vetList" {:vets {:vetList (db/get-vets)}})))

(defn show-owner [{{:keys [owner-id]} :route-params}]
  (page "owners/ownerDetails"  
        {:owner (db/show-owner owner-id)}))

(defn show-find-owners [_]
  (page "owners/findOwners" {:owner {:last_name ""}}))

(defn find-owners [{:keys [query-params]}]
  (let [owners (db/find-owner-by-last-name query-params)]
    (cond
      (= 1 (count owners))
      (response/redirect (bidi/path-for routes :show-owner :owner-id (-> owners first :id)))
      (>= 0 (count owners))
      (page "owners/findOwners" {:owner query-params})
      :else
      (page "owners/ownersList" {:selections owners}))))

(defn show-create-owner-form [_]
  (page "owners/createOrUpdateOwnerForm" 
      {:owner {:new true}
       :errors {}}))

(defn create-owner [{:keys [form-params]}]
  (process-if-valid
    :petclinic/owner
    form-params
    (fn [form]
      (let [new (db/create-owner form)]
        (response/redirect (bidi/path-for routes :show-owner
                              :owner-id (-> new first vals first)))))
    (fn [form errors]
      (page "owners/createOrUpdateOwnerForm"
        {:owner  form
         :errors errors}))))

(defn show-edit-owner-form [{{:keys [owner-id]} :route-params}]
  (page "owners/createOrUpdateOwnerForm"
        {:owner (db/show-owner owner-id)
         :errors {}}))

(defn edit-owner [{{:keys [owner-id]} :route-params
                   form-params :form-params}]
  (process-if-valid
    :petclinic/owner
    (assoc form-params :id owner-id)
    (fn [form]
      (db/edit-owner form)
      (response/redirect (bidi/path-for routes :show-owner
                            :owner-id owner-id)))
    (fn [form errors]
      (page "owners/createOrUpdateOwnerForm")
      {:owner (merge (db/show-owner owner-id) form)
       :errors errors})))

(defn show-add-pet-form [{{:keys [owner-id]} :route-params}]
  (page "pets/createOrUpdatePetForm"
        {:pet {:new true :owner (db/show-owner owner-id)}
         :errors {}
         :types (db/get-pet-types)}))

(defn add-pet [{{:keys [owner-id]} :route-params
                form-params :form-params}]
  (process-if-valid
    :petclinic/pet
    (-> form-params
        (assoc  :owner_id owner-id)
        (dissoc "id"))
    (fn [form] 
      (db/add-pet (str->int owner-id) form)
      (response/redirect (bidi/path-for routes :show-owner
                            :owner-id owner-id)))
    (fn [form errors]
      (page "pets/createOrUpdatePetForm"
        {:pet (assoc form :owner (db/show-owner owner-id))
         :errors errors
         :types (db/get-pet-types)}))))

(defn show-edit-pet-form [{{:keys [owner-id pet-id]} :route-params
                           form-params :form-params}]
  (page "pets/createOrUpdatePetForm"
    {:pet (db/show-pet (str->int owner-id)
                       (str->int pet-id))
     :errors {}
     :types (db/get-pet-types)}))

(defn edit-pet [{{:keys [owner-id pet-id]} :route-params
                 form-params :form-params}]
  (process-if-valid
    :petclinic/pet
    (assoc form-params
        :owner_id owner-id
        :id pet-id)
    (fn [form]
      (db/edit-pet form) 
      (response/redirect (bidi/path-for routes :show-owner :owner-id (:owner_id form))))
    (fn [form errors]
      (page "pets/createOrUpdatePetForm"
        {:pet (merge (db/show-pet (:owner_id form)
                                  (:pet_id form)) form)
         :errors errors
         :types (db/get-pet-types)}))))

(defn show-add-visit-form [{{:keys [owner-id pet-id]} :route-params}]
  (page "pets/createOrUpdateVisitForm"  
        {:visit {:new true}
         :errors {}
         :pet (db/show-pet (str->int owner-id)
                           (str->int pet-id))}))

(defn add-visit [{{:keys [owner-id pet-id]} :route-params
                  form-params :form-params}]
  (process-if-valid
    :petclinic/visit
    form-params
    (fn [form]
      (db/add-visit (str->int pet-id) form)
      (response/redirect (bidi/path-for routes :show-owner
                            :owner-id owner-id)))
    (fn [form errors]
      (page "pets/createOrUpdateVisitForm"
        {:visit form
         :errors errors
         :pet (db/show-pet (str->int owner-id)
                           (str->int pet-id))}))))

(defn server-error-page [req]
  (page "error" {:message (with-out-str (pp/pprint req))}))

(def routes
  ["/"
    {#{"" "/"} #'welcome
     ["vets." :export-format] #'vets
     "owners" 
      {#{"" "/"} #'find-owners
        "/new" {:get #'show-create-owner-form
                :post #'create-owner}
        "/find" {:get #'show-find-owners
                 :post #'find-owners}
        ["/" :owner-id]  
        {"" (-> #'show-owner (bidi/tag :show-owner))
         "/edit" {:get #'show-edit-owner-form
                   :post #'edit-owner}
         "/pets" {"/new" {:get #'show-add-pet-form
                          :post #'add-pet}
                  ["/" :pet-id] 
                  {"/edit" {:get #'show-edit-pet-form 
                            :post #'edit-pet}
                   "/visits/new" {:get #'show-add-visit-form 
                                  :post #'add-visit}}}}}}])
