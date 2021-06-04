(ns rds-utlilization.core
       (:import org.joda.time.DateTime
                java.text.SimpleDateFormat
                java.util.Date)
       (:require [amazonica.aws.rds :as rds]
                 [amazonica.aws.cloudwatch :as cw]
                 [clojure.string :as str])
       (:gen-class))

;; definition used for period specification
(def DAILY (* 60 60 24))

;; define credentials for accessing AWS API 
(def cred 
  {:access-key 
   (or (System/getenv "AWS_ACCESS_KEY_ID")
       "<put your secret key id here>") 
   :secret-key 
   (or (System/getenv "AWS_SECRET_ACCESS_KEY")
       "<put your secret access key here")
   :session-token 
   (or (System/getenv "AWS_SESSION_TOKEN")
       "put your secret session token here")})

(def cred-us (assoc cred :endpoint "us-east-1"))
(def cred-eu (assoc cred :endpoint "eu-west-1"))
(def cred-apac (assoc cred :endpoint "ap-northeast-1"))

;; get all RDS instances in the region, handling the "marker" logic correctly
(defn rds-instances [cred]
  (loop [marker nil result []]
    (let [resp
          (rds/describe-db-instances cred :marker marker)]
      (if (nil? (:marker resp))
        (into result (:dbinstances resp))
        (recur (:marker resp) (into result (:dbinstances resp)))))))

;; get all RDS clusters in a given region, handling the "marker" logic correctly
(defn rds-clusters [cred]
  (loop [marker nil result []]
    (let [resp
          (rds/describe-db-clusters cred :marker marker)]
      (if (nil? (:marker resp))
        (into result (:dbclusters resp))
        (recur (:marker resp) (into result (:dbclusters resp)))))))

;; return the list of the dbinstance-ids
(defn get-instances-ext [credentials]
  (map #(vector (:dbinstance-identifier %)
                (:dbinstance-class %))
       (rds-instances credentials)))

;; equivalent command to get-metric-statistics: 
;;    $ aws cloudwatch get-metric-statistics \
;;        --metric-name CPUUtilization \
;;        --region us-east-1 \
;;        --namespace="AWS/RDS" \
;;        --start-time 2019-06-01T00:00:00 \
;;        --end-time 2019-06-02T00:00:00 \
;;        --statistics=Average \
;;        --period 3600 \
;;        --dimensions "Name=DBInstanceIdentifier,Value=db01"
;; 
(defn get-metrics-for-instance-id [credentials instance-id period days]
  (cw/get-metric-statistics credentials
                            :metric-name "CPUUtilization"
                            :namespace "AWS/RDS"
                            :start-time (.minusDays (DateTime.) days)
                            :end-time (DateTime.)
                            :period period
                            :dimensions  [{ :name "DBInstanceIdentifier" 
                                           :value instance-id }]
                            :statistics ["Average" "Maximum" "Minimum"]))

;; destructuring loop
(defn tloop [n]
  (loop [[h & t] n 
         res []]
    (if (nil? h)
      res
      (recur t (into res (vector (+ 1 h)))))))

;;
;; helper functions needed to overcome the lack of nested lambda functions
;;
(def DAILY (* 60 60 24))

;;
;; calculate average: [Num a] -> Num a
;;  when passed empty vector returns 0
;;
(defn average
  [numbers]
  (if (empty? numbers)
    0
    (/ (apply + numbers) (count numbers))))

(defn get-max [l] (map #(:max %) l))
(defn get-avg [l] (map #(:avg %) l))

(defn format-metrics-data [data]
  (let [str-format (str/join " " (map #(format "%%6.1f" %) data))]
    (apply format str-format 
           (map #(* 1.0 %) data))))

(defn get-single-cpu 
  ";; gets cpu {avg,max} statistics for a given instance-id from CloudWatch. Function accepts
  ;; following parameters:
  ;;    credentials - as AWS credentials map
  ;;    instance-id - AWS RDS instance-id
  ;;    period      - period of aggregation in seconds, where (* 60 60 24) means DAILY
  ;;    days        - number of days back in history from now"  
  [credentials instance-id period days]
  (map #(hash-map :avg (:average %) :max (:maximum %)) 
       (sort-by :timestamp  
                (:datapoints (get-metrics-for-instance-id 
                              credentials 
                              instance-id 
                              period 
                              days)))))

(defn get-cpu-ext 
  ";; gets extended {max,avg} cpu usage along with rds class, when passed following parameters:
  ;;    credentials - as AWS credentials map
  ;;    period      - period of aggregation in seconds, where (* 60 60 24) means DAILY
  ;;    days        - number of days back in history from now
  ;;
  ;; Exmaple call: (get-cpu-ext cred-us DAILY 14)"
  [credentials period days]
  (sort #(compare (:max (second %2)) (:max (second %1)))
        (map #(let [id (first %)
                    class (second %)
                    cpu (get-single-cpu credentials id period days)]  
                (vector id 
                        (hash-map
                         :class class
                         :max (apply max (concat '(0) (get-max cpu)))
                         :avg (average (vec (get-avg cpu)))
                         :data (format-metrics-data (vec (get-avg cpu))))))
             (get-instances-ext credentials))))

(defn report-ext [credentials]
  (doall
   (mapcat #(println 
             (format "%-48s %-16s %6.1f%% %6.1f%% | %s" 
                     (first %) 
                     (:class (second %)) 
                     (:max (second %)) 
                     (:avg (second %))
                     (:data (second %))))
           (get-cpu-ext credentials DAILY 7))))

(defn -main [& args]
  (println " ")
  (println "---- US Region ----")
  (report-ext cred-us)

  (println " ")
  (println "---- EU Region ----")
  (report-ext cred-eu)

  (println " ")
  (println "---- APAC Region ----")
  (report-ext cred-apac))
