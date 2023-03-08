(ns org.parkerici.sample-tracking.db.schema
  (:require [org.parkerici.multitool.cljcore :as u]
            [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.alzabo.schema :as schema]
            [org.parkerici.alzabo.datomic :as datomic]))

;;; NOTE: whenever this changes, run the function (transact-schema) to update all databases.
;;; Also note that some schema changes are outlawed by Datomic, so verify it works.

;;; This uses the schema format from Alzabo: https://github.com/ParkerICI/alzabo/blob/master/src/alzabo/schema.clj

(def schema
  {:kinds
   {:study
    {:fields {:uuid                              {:type      :uuid
                                                  :unique-id true
                                                  :required  true
                                                  :doc       "The UUID for the entity"}
              :name                              {:type      :string
                                                  :unique-id true
                                                  :required  true
                                                  :doc       "The name of the study (e.g. PICI0002)."}
              :active                            {:type     :boolean
                                                  :required true
                                                  :doc      "If this entity is active and should be displayed to users"}
              :create-time                       {:type     :instant
                                                  :required true
                                                  :doc      "The time this entity was created"}
              :participant-id-prefix             {:type :string
                                                  :doc  "An initial prefix for this study's participant-ids"}
              :participant-id-regex              {:type :string
                                                  :doc  "A regex for validating participant-ids"}
              :participant-id-validation-message {:type :string
                                                  :doc  "A message to display for invalid participant-ids"}
              :kit-id-prefix                     {:type :string
                                                  :doc  "An initial prefix for this study's kit-ids"}
              :kit-id-regex                      {:type :string
                                                  :doc  "A regex for validating kit-ids"}
              :kit-id-validation-message         {:type :string
                                                  :doc  "A message to display for invalid kit-ids"}
              :cohorts                           {:type        [:cohort :boolean]
                                                  :cardinality :many
                                                  :doc         "The cohorts of participants in the study and their active statuses."}
              :sites                             {:type        [:site :boolean]
                                                  :cardinality :many
                                                  :doc         "The sites a study is running at and their active statuses."}}}

    :site
    {:fields {:uuid        {:type      :uuid
                            :unique-id true
                            :required  true
                            :doc       "The UUID for the entity"}
              :name        {:type      :string
                            :required  true
                            :unique-id true
                            :doc       "The name of a site"}
              :create-time {:type     :instant
                            :required true
                            :doc      "The time this entity was created"}}}

    :cohort
    {:fields {:uuid        {:type      :uuid
                            :unique-id true
                            :required  true
                            :doc       "The UUID for the entity"}
              :name        {:type     :string
                            :required true
                            :doc      "The name of a cohort of participants (e.g. Default or Cohort B)."}
              :study       {:type     :study
                            :required true
                            :doc      "The study this cohort belongs to"}
              :create-time {:type     :instant
                            :required true
                            :doc      "The time this entity was created"}
              :kit-types   {:type        [:kit-type :boolean]
                            :cardinality :many
                            :doc         "The types of kits that may be used for a cohort and their active statues."}}}

    :kit-type
    {:fields {:uuid                     {:type      :uuid
                                         :unique-id true
                                         :required  true
                                         :doc       "The UUID for the entity"}
              :name                     {:type     :string
                                         :required true
                                         :doc      "The name of a kit type (e.g. Blood Collection Kit)."}
              :create-time              {:type     :instant
                                         :required true
                                         :doc      "The time this entity was created"}
              :collection-date-required {:type     :boolean
                                         :required true
                                         :doc      "Whether or not collection timestamp is required for this kit"}
              :air-waybill-required     {:type     :boolean
                                         :required true
                                         :doc      "Whether or not air waybill is required for this kit"}
              :vendor-email             {:type :string
                                         :doc  "The email address for the vendor this kit belongs to."}
              :item-number              {:type      :long
                                         :required  true
                                         :unique-id true
                                         :doc       "The external ID for a kit-type. Used as a unique ID and to join with Therapak."}
              :sample-types             {:type        :sample-type
                                         :component   true
                                         :cardinality :many
                                         :doc         "A kit usually has multiple samples collected for it. These are the types of samples that can be collected for this type of kit."}
              :timepoints               {:type        :timepoint
                                         :component   true
                                         :cardinality :many
                                         :doc         "The timepoints this kit can be used for (e.g. Cycle 1 Day 1)"}
              :form-type                {:type        :form-type
                                         :component   true
                                         :cardinality :one
                                         :doc         "Form-type stores custom form field definitions that need to be collected for a kit. Depending on how the kit is being used, a different form might need to be filled out."}}}

    :timepoint
    {:fields {:uuid {:type      :uuid
                     :unique-id true
                     :required  true
                     :doc       "The UUID for the entity"}
              :name {:type      :string
                     :unique-id true
                     :required  true
                     :doc       "The name of the timepoint (e.g. Cycle 2 Day 3)."}}}

    :sample-type
    {:fields {:uuid             {:type      :uuid
                                 :unique-id true
                                 :required  true
                                 :doc       "The UUID for the entity"}
              :name             {:type     :string
                                 :required true
                                 :doc      "The name of a sample type (e.g. Red Cap Serum)"}
              :id-suffix        {:type     :string
                                 :required true
                                 :doc      "A sample has a two part ID. The first part is the kit id that it belongs two, and the second part is this generic id-suffix."}
              :ships-with-kit   {:type     :boolean
                                 :required true
                                 :doc      "If true, this sample is shipped out immediately by the site once the kit has been used. If false, the sample is kept by the site and might ship at a later date."}
              :reminder         {:type :string
                                 :doc  "A reminder about the sample to be displayed to the user. E.g. Please place tube only in Primary Bag or Cryobox"}
              :attribute-values {:type        :sample-attribute-value
                                 :component   true
                                 :cardinality :many
                                 :doc         "Attributes for a sample (e.g. primary vs backup)"}}}

    :sample-attribute
    {:fields {:uuid {:type      :uuid
                     :unique-id true
                     :required  true
                     :doc       "The UUID for the entity"}
              :name {:type     :string
                     :required true?
                     :doc      "The name of a sample attribute"}}}

    :sample-attribute-value
    {:fields {:uuid      {:type      :uuid
                          :unique-id true
                          :required  true
                          :doc       "The UUID for the entity"}
              :name      {:type     :string
                          :required true?
                          :doc      "The name of a sample attribute value"}
              :attribute {:type        :sample-attribute
                          :cardinality :one
                          :required    true
                          :doc         "The attribute this value belongs to"}}}

    :form-type
    {:fields {:uuid   {:type      :uuid
                       :unique-id true
                       :required  true
                       :doc       "The UUID for the entity"}
              :name   {:type      :string
                       :required  true
                       :unique-id true
                       :doc       "The name of a form. Different sample types can share the same form"}
              :fields {:type        :form-type-field
                       :cardinality :many
                       :component   true
                       :required    true
                       :doc         "The fields that belong to this form-type"}}}

    :form-type-field
    {:fields {:uuid       {:type      :uuid
                           :unique-id true
                           :required  true
                           :doc       "The UUID for the entity"}
              :field-id   {:type     :string
                           :required true
                           :doc      "The id used for this field on forms"}
              :field-type {:type      :form-type-field-type
                           :component true
                           :required  true
                           :doc       "The type of field this is (e.g. boolean, int, time, string, select)"}
              :required   {:type     :boolean
                           :required true
                           :doc      "Whether or not this field is required"}
              :label      {:type     :string
                           :required true
                           :doc      "The label for this field"}
              :options    {:type        [:string :string]
                           :cardinality :many
                           :doc         "If this field is a select field, tuples of (id, value) for the select field"}}}

    :kit
    {:fields {:uuid                  {:type      :uuid
                                      :unique-id true
                                      :required  true
                                      :doc       "The UUID for the entity"}
              :kit-id                {:type     :string
                                      :required true
                                      :doc      "The string identifier for this kit."}
              :participant-id        {:type :string
                                      :doc  "The ID of the participant that this kit is being used to collect samples from."}
              :collection-timestamp  {:type :instant
                                      :doc  "A timestamp for when this kit was collected. Defined by the person entering data into the form."}
              :timezone              {:type     :string
                                      :required true
                                      :doc      "The timezone for proper rendering of the timestamps that belong to this kit (e.g. America/Los_Angeles)"}
              :completing-first-name {:type :string
                                      :doc  "The first name of the person completing this kit."}
              :completing-last-name  {:type :string
                                      :doc  "The last name of the person completing this kit."}
              :completing-email      {:type :string
                                      :doc  "The email address of the person completing this kit."}
              :comments              {:type :string
                                      :doc  "Comments about the kit."}
              :complete              {:type     :boolean
                                      :required true
                                      :doc      "Whether or not this kit has been completed and submitted by the site."}
              :site                  {:type        :site
                                      :cardinality :one
                                      :required    true
                                      :doc         "The site this kit was collected for."}
              :cohort                {:type        :cohort
                                      :cardinality :one
                                      :required    true
                                      :doc         "The cohort this kit was collected for."}
              :timepoints            {:type        :timepoint
                                      :cardinality :many
                                      :required    true
                                      :doc         "The timepoint this kit was collected for."}
              :kit-type              {:type        :kit-type
                                      :cardinality :one
                                      :required    true
                                      :doc         "The kit-type that this kit is an instance of."}
              :samples               {:type        :sample
                                      :component   true
                                      :cardinality :many
                                      :doc         "The samples that were collected for this kit."}
              ; We can get a kit's shipments through samples, but also keep an explicit reference so that we don't lose
              ; shipments associated with kits that don't have samples marked as shipped (can happen during the kit
              ; sharing process or possibly if a user accidentally doesn't mark any samples in a kit as shipped).
              ; Feels a little hacky, but best solution I have for now.
              ; Maybe disable air waybill field unless a sample is marked as shipped, but could be confusing for users?
              :shipments             {:type        :shipment
                                      :cardinality :many
                                      :required    false
                                      :doc         "Any shipments associated with this kit"}
              :form-values           {:type        :form-value
                                      :component   true
                                      :cardinality :many
                                      :doc         "The values for the custom form fields defined in form-type collected for this kit."}
              :submission-timestamp  {:type :instant
                                      :doc  "A timestamp for when this kit was submitted."}
              :archived              {:type :boolean
                                      :doc  "If this kit has been archived"}}}

    :sample
    {:fields {:uuid        {:type      :uuid
                            :unique-id true
                            :required  true
                            :doc       "The UUID for the entity"}
              :sample-id   {:type     :string
                            :required :true
                            :doc      "The barcoded ID on this sample"}
              :collected   {:type :boolean
                            :doc  "If this sample was collected"}
              :shipped     {:type :boolean
                            :doc  "If this sample was shipped"}
              :sample-type {:type        :sample-type
                            :cardinality :one
                            :required    true
                            :doc         "The sample-type that this sample is an instance of."}
              :shipment    {:type        :shipment
                            :cardinality :one
                            :required    false
                            :doc         "The shipment that this sample was shipped in"}}}

    :form-value
    {:fields {:uuid          {:type      :uuid
                              :unique-id true
                              :required  true
                              :doc       "The UUID for the entity"}
              :field         {:type        :form-type-field
                              :cardinality :one
                              :required    true
                              :doc         "The form-type-field that this value belongs to"}
              :value_string  {:type :string
                              :doc  "If this field is a string, then value will be stored here."}
              :value_long    {:type :long
                              :doc  "If this field is a long, then value will be stored here."}
              :value_float   {:type :float
                              :doc  "If this field is a float, then value will be stored here."}
              :value_instant {:type :instant
                              :doc  "If this field is an instant (time), then value will be stored here."}
              :value_boolean {:type :boolean
                              :doc  "If this field is a boolean, then value will be stored here."}}}

    :shipment
    {:fields {:uuid        {:type      :uuid
                            :unique-id true
                            :required  true
                            :doc       "The UUID for the entity"}
              :air-waybill {:type     :string
                            :required true}
              :archived    {:type :boolean
                            :doc  "If this shipment has been archived"}}}

    :proposed-kit-edit
    {:fields {:uuid           {:type      :uuid
                               :unique-id true
                               :required  true
                               :doc       "The UUID for the entity"}
              :kit            {:type        :kit
                               :cardinality :one
                               :required    true
                               :doc         "The kit that this is proposing to edit"}
              :update-map     {:type     :string
                               :required true
                               :doc      "A stringified kit map that can be used to update the kit via the kit-shipment api"}
              :status         {:type      :kit-edit-status
                               :component true
                               :required  true
                               :doc       "The status of this edit (e.g. pending, approved, or denied)"}
              :user           {:type        :user
                               :cardinality :one
                               :required    true
                               :doc         "The user proposing the edit."}
              :time           {:type     :instant
                               :required true
                               :doc      "The time this edit was proposed."}
              :reviewing-user {:type        :user
                               :cardinality :one
                               :doc         "The user proposing the edit."}}}

    :role
    {:fields {:uuid {:type      :uuid
                     :unique-id true
                     :required  true
                     :doc       "The UUID for the entity"}
              :name {:type      :string
                     :unique-id true
                     :required  true}}}

    :user
    {:fields {:uuid        {:type      :uuid
                            :unique-id true
                            :required  true
                            :doc       "The UUID for the entity"}
              :email       {:type      :string
                            :unique-id true
                            :required  true}
              :deactivated {:type :boolean}
              :roles       {:type        :role
                            :cardinality :many
                            :required    true
                            :doc         "The roles that this user belongs to"}}}

    :history
    {:fields {:uuid        {:type      :uuid
                            :unique-id true
                            :required  true
                            :doc       "The UUID for the entity"}
              :agent-email {:type     :string
                            :required true
                            :doc      "The email address of the agent making the change"}
              :time        {:type     :instant
                            :required true
                            :doc      "The time this change was made"}
              :entity-type {:type     :keyword
                            :required true
                            :doc      "The type of entity being changed"}
              :entity-uuid {:type     :uuid
                            :required true
                            :doc      "The id of the database entity being changed"}
              :old-value   {:type     :string
                            :required true
                            :doc      "The old value of the entity being changed"}
              :new-value   {:type :string
                            :doc  "The new value of the entity being changed. Can be blank in the case of deletion"}}}

    :migration
    {:fields {:uuid {:type      :uuid
                     :unique-id true
                     :required  true
                     :doc       "The UUID for the entity"}
              :name {:type      :string
                     :unique-id true
                     :require   true
                     :doc       "The name of the migration"}
              :time {:type    :instant
                     :require true
                     :doc     "The time the migration was made"}}}}

   :enums {
           :form-type-field-type
           {:values #:form-type-field-type{:boolean "boolean"
                                           :int     "int"
                                           :time    "time"
                                           :select  "select"
                                           :string  "string"}}
           :kit-edit-status
           {:values #:kit-edit-status{:pending  "pending"
                                      :approved "approved"
                                      :denied   "denied"}}}})

(defn write-schema-file
  [datomic-schema]
  (u/schpit "resources/schema.edn" datomic-schema))

(defn transact-schema
  []
  (let [datomic-schema (datomic/datomic-schema (schema/validate-schema schema))]
    (write-schema-file datomic-schema)
    (d/wrap-datomic-fn #(d/transact datomic-schema))))