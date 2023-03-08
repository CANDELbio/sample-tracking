(ns org.parkerici.sample-tracking.utils.collection)

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn merge-map-colls
  "Takes two collections of maps: map-coll, and joining-map-coll.
  For each element of map-coll, finds all the elements in joining-map-coll whose join-id-key matches the map-id-key for
  that element in map-coll and adds them as a list under the key add-key."
  [map-coll map-id-key joining-map-coll join-id-key add-key]
  (let [joining-map (reduce (fn [m value]
                              (let [id (get value join-id-key)
                                    cur-values (or (get m id) [])
                                    value-without-joining-key (dissoc value join-id-key)
                                    updated-values (conj cur-values value-without-joining-key)]
                                (assoc m id updated-values))) {} joining-map-coll)]
    (into [] (for [elem map-coll] (assoc elem add-key (get joining-map (get elem map-id-key)))))))