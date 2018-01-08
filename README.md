# Pet Clinic

Revamp [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) into Clojure. 
It is intended for developer to compare with the original PetClinic to get a feeling of how
Clojure saving Java Developer from the black magic world but still keeping the productivity.

## Basic Structure

### Data Model

Here is the Model used in PetClinic, presenting its schema using JSON

    BaseEntity
    \- NamedEntity
    \-- Pet
    \-- PetType
    \-- Specialty
    \- Person
    \-- Owner
    \-- Vet
    \- Visit


|  Type    |   JSON sample |
|----------|---------------|
| Pet      |{"id":1,"name":null,"birth_date":"yyyy-MM-dd","type_id":1,"owner_id":1,"visits":[]} |
| PetType  |{"id":1,"name":null}|
| Specialty|{"id":1,"name":null}|
| Owner    |{"id":1,"first_name":"","last_name":"","address":"","city":"","telephone":"\d{10}","pets":[]} |
| Vet      | {"id":1,"first_name":"","last_name":"","specialties":[]} |
| Visit    | {"id":1,"date":"yyyy-MM-dd","description":"","pet_id":1} |


### Routes

    ; create owner
    GET /owners/new
    POST /owners/new
    ; search/list/get owner
    GET /owners/find
    GET /owners
    GET /owners/{ownerId}
    ; update owner
    GET /owners/{ownerId}/edit
    POST /owners/{ownerId}/edit

    ; create pet
    GET /owners/{ownerId}/pets/new
    POST /owners/{ownerId}/pets/new
    ; view pet
    GET /owners/{ownerId}/pets/{petId}
    ; update pet
    GET /owners/{ownerId}/pets/{petId}/edit
    POST /owners/{ownerId}/pets/{petId}/edit

    ; create visit
    GET /owners/{ownerId}/pets/{petId}/visits/new
    POST /owners/{ownerId}/pets/{petId}/visits/new

    ; others
    GET /vets.html
    GET /vets.json
    GET /vets.xml
    GET /

## Get Started

### Install Clojure commandline

    brew install clojure

### Create db and seed data

    clj init-db.clj

### Compile LESS to CSS

    clj -R:less less.clj

### REPL

    clj -R:dev 

### Start app server in development mode (live code reloading)

    clj -R:dev -m petclinic.core dev

### Start app server in production mode

    clj -m petclinic.core prod

### Trying the project without installing any extra editor

    clj -R:dev -R:editor -m nightlight.core
    open http://localhost:4000/


## Incompatible changes

* Remove Spring framework usage, like spring-thymeleaf integration will be removed 
* Remove hibernate usage, the field names will follow db table's naming.
* LESS compilation/DB Migration are individual clj scripts to demostrate the Clojure scripting feature

## Report Bugs

It is just a weekend project. It might contains quite lots of bugs. Feel free to report them in Github, thanks.

## License

The Clojure PetClinic sample application is released under version 2.0 of the Apache License. Same as Spring PetClinic sample application. All resources are directly copied from Spring PetClinic sample application with necessary modifications. 