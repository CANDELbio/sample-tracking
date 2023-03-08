;;; Run by CI to create a local database

(require 'datomic.api)
(datomic.api/create-database "datomic:dev://localhost:4334/ereq-test")
