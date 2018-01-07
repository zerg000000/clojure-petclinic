(ns petclinic.db
  (:require [clojure.java.jdbc :as j]
            [clojure.walk :as walk]))

(def db-spec {:connection-uri "jdbc:hsqldb:file:testdb"})

(defn new-owner []
  {:new true})

(defn create-owner [owner]
  (let [new-owner (dissoc owner :id)]
    (j/insert! db-spec :owners owner)))

(defn edit-owner [{:keys [id] :as owner}]
  (let [new-owner (dissoc owner :id)]
    (j/update! db-spec :owners new-owner ["id = ?" id])))

(defn show-owner [owner-id]
  (let [owner (first (j/query db-spec ["select * from owners where id = ?" owner-id]))
        raw-pets (j/query db-spec ["select p.*, t.name as type, v.pet_id, v.visit_date, v.description
                                           from pets p 
                                           left join types t on p.type_id = t.id 
                                           left join visits v on p.id = v.pet_id
                                           where owner_id = ?" owner-id])
        pets-groups (group-by #(select-keys % [:id :name :type :birth_date :owner_id :type_id]) raw-pets)
        pets (map (fn [v]
                    (let [sp (map #(select-keys % [:visit_date :pet_id :description]) 
                                  (get pets-groups v))]
                      (assoc v :visits sp)))
               (keys pets-groups))]
    (assoc owner :pets pets)))

(defn find-owner-by-last-name [search-form]
  (let [owners (j/query db-spec ["select * 
                                    from owners 
                                   where last_name like ? || '%' " 
                                 (or (get search-form "last_name") "")])]
    (map #(assoc % :pets (j/query db-spec ["select * 
                                              from pets 
                                             where owner_id = ?" (:id %)])) owners)))

(defn show-pet [owner-id pet-id]
  (let [pet (first (j/query db-spec ["select p.*, t.name as type 
                                        from pets p 
                                        left join types t 
                                          on p.type_id = t.id 
                                       where p.id = ? 
                                         and p.owner_id = ?" pet-id owner-id]))]
    (assoc pet 
      :owner (first (j/query db-spec ["select * 
                                         from owners 
                                        where id = ?" owner-id]))
      :visits (j/query db-spec ["select * 
                                   from visits 
                                  where pet_id = ?" pet-id]))))

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
                                  on vs.specialty_id = s.id")
        raw-vets (group-by #(select-keys % [:first_name :last_name :id]) raw)
        vets (map (fn [v]
                    (let [sp (map #(select-keys % [:name]) 
                                  (get raw-vets v))]
                      (assoc v :specialties sp)))
               (keys raw-vets))]
    vets))

(defn add-visit [pet-id visit]
  (let [new-visit (-> visit
                    (walk/keywordize-keys) 
                    (assoc :pet_id pet-id)
                    (dissoc :id))]
    (j/insert! db-spec :visits new-visit)))


