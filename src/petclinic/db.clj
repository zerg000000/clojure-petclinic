(ns petclinic.db
  (:require [clojure.java.jdbc :as j]
            [clojure.walk :as walk]
            [clojure.set :as set])
  (:import [org.hsqldb.jdbc JDBCPool]))

(defn row->obj [rows spec]
  (let [{:keys [cols names specs single]} spec
        renames (if names (zipmap cols names))
        objs-rows (group-by #(select-keys % cols) rows)
        rs (map
             (fn [[obj rows-per-obj]]
               (let [o (into obj
                         (for [[k spec-body] specs]
                           [k (row->obj rows-per-obj spec-body)]))]
                 (if renames
                   (set/rename-keys o renames)
                   o)))
             objs-rows)]
    (if single
      (first rs)
      rs)))

;(def db-spec {:connection-uri "jdbc:hsqldb:file:testdb"})  
(def db-spec {:datasource (doto (JDBCPool.)
                                (.setUrl "jdbc:hsqldb:file:testdb"))})

(defn create-owner [owner]
  (let [new-owner (dissoc owner :id)]
    (j/insert! db-spec :owners owner)))

(defn edit-owner [{:keys [id] :as owner}]
  (let [new-owner (dissoc owner :id)]
    (j/update! db-spec :owners new-owner ["id = ?" id])))

(defn show-owner [owner-id]
  (let [raw-owners (j/query db-spec ["select o.id o_id,
                                             o.first_name o_first_name, 
                                             o.last_name o_last_name,
                                             o.address o_address,
                                             o.city o_city,
                                             o.telephone o_telephone,
                                             p.*, 
                                             t.name as type, v.pet_id, v.visit_date, v.description
                                      from owners o
                                      left join pets p on p.owner_id = o.id 
                                      left join types t on p.type_id = t.id 
                                      left join visits v on p.id = v.pet_id
                                      where p.owner_id = ?" owner-id])]
    (row->obj raw-owners
      {:cols [:o_id :o_first_name :o_last_name :o_address :o_city :o_telephone]
       :names [:id :first_name :last_name :address :city :telephone]
       :single true
       :specs {:pets
               {:cols [:id :name :type :birth_date :owner_id :type_id]
                :specs {:visits {:cols [:visit_date :pet_id :description]}}}}})))

(defn find-owner-by-last-name [search-form]
  (let [owners (j/query db-spec ["select o.id o_id,
                                         o.first_name o_first_name, 
                                         o.last_name o_last_name,
                                         o.address o_address,
                                         o.city o_city,
                                         o.telephone o_telephone,
                                         p.*, 
                                         t.name as type
                                    from owners o
                                    left join pets p on p.owner_id = o.id 
                                    left join types t on p.type_id = t.id  
                                   where o.last_name like ? || '%' " 
                                 (or (get search-form "last_name") "")])]
    (row->obj owners
      {:cols [:o_id :o_first_name :o_last_name :o_address :o_city :o_telephone]
       :names [:id :first_name :last_name :address :city :telephone]
       :specs {:pets
               {:cols [:id :name :type :birth_date :owner_id :type_id]}}})))    

(defn show-pet [owner-id pet-id]
  (let [pet (j/query db-spec 
             ["select o.id o_id,
                      o.first_name o_first_name, 
                      o.last_name o_last_name,
                      o.address o_address,
                      o.city o_city,
                      o.telephone o_telephone,
                      p.*, 
                      t.name as type, v.pet_id, v.visit_date, v.description 
                 from pets p
                 left join owners o
                   on o.id = p.owner_id
                 left join types t 
                   on p.type_id = t.id
                 left join visits v
                   on v.pet_id = p.id
                where p.id = ? 
                  and p.owner_id = ?" 
              pet-id owner-id])]
    (row->obj pet
      {:cols [:id :name :type :birth_date :owner_id :type_id]
       :single true
       :specs {:owner
               {:cols [:o_id :o_first_name :o_last_name :o_address :o_city :o_telephone]
                :single true
                :names [:id :first_name :last_name :address :city :telephone]}
               :visits
               {:cols [:pet_id :visit_date :description]}}})))   

(defn get-pet-types []
  (j/query db-spec "select * from types"))

(defn edit-pet [{:keys [id owner_id] :as pet}]
  (let [new-pet (dissoc pet :id)]
    (j/update! db-spec :pets new-pet ["id = ? and owner_id = ?" id owner_id])))

(defn add-pet [owner-id pet]
  (let [new-pet (-> pet
                    (walk/keywordize-keys) 
                    (assoc :owner_id owner-id)
                    (dissoc :id))]
    (j/insert! db-spec :pets new-pet)))

(defn get-vets []
  (let [raw (j/query db-spec "select v.*, s.name 
                                from vets v 
                                left join vet_specialties vs
                                  on v.id = vs.vet_id
                                left join specialties s
                                  on vs.specialty_id = s.id")]
    (row->obj raw {:cols [:first_name :last_name :id]
                   :specs {:specialties {:cols [:name]}}})))

(defn add-visit [pet-id visit]
  (let [new-visit (-> visit
                    (walk/keywordize-keys) 
                    (assoc :pet_id pet-id)
                    (dissoc :id))]
    (j/insert! db-spec :visits new-visit)))