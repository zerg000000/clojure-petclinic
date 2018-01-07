(ns petclinic.specs
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as string]
            [phrase.alpha :refer [defphraser phrase]])
  (:import [java.text SimpleDateFormat]))

(def date-format "yyyy-MM-dd")

(defn str->date [date-str]
  (let [formatter (SimpleDateFormat. date-format)]
    (try
      (.parse formatter date-str)
      (catch Exception ex
        ::spec/invalid))))

(defn date->str [date]
    (let [formatter (SimpleDateFormat. date-format)]
        (.format formatter date)))

(defn str->int [int-str]
  (if (string? int-str)
    (Integer/parseInt int-str)
    int-str))

(defn int->str [int-num]
  (str int-num))

(defn is-date-string? [string]
  (re-find #"^\d{4}-\d{2}-\d{2}$" string))

(spec/def :field/date
  (spec/and
      is-date-string?
      (spec/conformer str->date date->str)
      inst?))

(spec/def :field/id
  (spec/and
      #(re-find #"^\d+$" %)
      (spec/conformer str->int int->str)
      pos-int?))

(spec/def :field/non-blank-text
  (spec/and
      string?
      (comp not string/blank?)))

(spec/def :field/telephone
  (spec/and
    string?
    #(re-find #"^\+{0,1}[\d ]+$" %)))

(spec/def :pet/name :field/non-blank-text) 
(spec/def :pet/birth_date :field/date)
(spec/def :pet/type_id :field/id)
(spec/def :pet/type string?)
(spec/def :pet/owner_id :field/id)
(spec/def :pet/id :field/id)

(spec/def :petclinic/pet
  (spec/keys :req-un [:pet/name :pet/birth_date :pet/type_id :pet/owner_id]
             :opt-un [:pet/id :pet/type]))

(spec/def :visit/visit_date :field/date)
(spec/def :visit/description :field/non-blank-text)

(spec/def :petclinic/visit
  (spec/keys :req-un [:visit/description :visit/visit_date]))

(spec/def :owner/first_name :field/non-blank-text)
(spec/def :owner/last_name :field/non-blank-text)
(spec/def :owner/address :field/non-blank-text)
(spec/def :owner/city :field/non-blank-text)
(spec/def :owner/telephone :field/telephone)

(spec/def :petclinic/owner
  (spec/keys :req-un [:owner/first_name :owner/last_name :owner/address :owner/city :owner/telephone]))

(defphraser is-date-string?
  [_ _]
  (str "Invalid date format. " date-format))

(defphraser (comp not string/blank?)
  [_ {:keys [path]}]
  (str (name (first path)) " could not be blank"))

(defphraser (spec/conformer str->int int->str)
  {:via [:pet/owner_id]}
  [_ _]
  "Owner id on should be a number")

(defphraser #(re-find re %)
  {:via [:petclinic/owner :field/telephone]}
  [_ _ re]
  "Invalid telephone number. should be +000 9888 2222 or 9999 9999")
(defphraser :default
  [_ _]
  "Invalid value!")

(defn error-message [form explain]
  (let [problems (:clojure.spec.alpha/problems explain)]
    (into {}
      (map (fn [p]
            [(-> p :path first) (phrase form p)]) 
          problems))))
