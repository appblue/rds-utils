(defn events [cred days]
  (loop [marker nil result []]
    (let [resp
          (rds/describe-events
           cred
           :marker marker
           :event-categories ["configuration change"]
           :source-type "db-instance"
           :duration (* 24 60 days))]
      (if (nil? (:marker resp))
        (into result (:events resp))
        (recur (:marker resp) (into result (:events resp)))))))

(defn print-all-events [cred days]
  (mapcat #(println
            (.toString (:date %) "yyyy-MM-dd HH:mm:ss")
            (:source-identifier %)
            (:message %))
          (remove #(re-matches #"^backup-rds-exter.*" (:source-identifier %))
                  (events cred days))))
