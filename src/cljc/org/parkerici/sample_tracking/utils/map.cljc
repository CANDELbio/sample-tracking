(ns org.parkerici.sample-tracking.utils.map)

(defn get-key
  [prefix key]
  (if (nil? prefix)
    key
    (str prefix "-" key)))

(defn flatten-map-kvs
  ([map] (flatten-map-kvs map nil))
  ([map prefix]
   (reduce
     (fn [memo [k v]]
       (if (map? v)
         (concat memo (flatten-map-kvs v (get-key prefix (name k))))
         (conj memo [(get-key prefix (name k)) v])))
     [] map)))

; Taken from https://gist.github.com/sudodoki/023d5f08c2f847b072b652687fdb27f2
; Given a nested map, flattens into a single layer map and joins the keys with dashes.
(defn flatten-map
  [m]
  (into {} (flatten-map-kvs m)))
