(defn get-rds-snapshots [cred dbid]
  (loop [marker nil result []]
    (let [resp
          (rds/describe-db-snapshots cred :marker marker :dbinstance-identifier dbid)]
      (if (nil? (:marker resp))
        (into result (:dbsnapshots resp))
        (recur (:marker resp) (into result (:dbsnapshots resp)))))))

;; 
;; print out all snapshots for a given dbid 
;; 
(defn snapshots [cred dbid]
  (mapcat #(println (:dbsnapshot-identifier %) (:snapshot-type %)) 
          (get-rds-snapshots cred dbid)))
