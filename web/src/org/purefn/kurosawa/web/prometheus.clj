(ns org.purefn.kurosawa.web.prometheus
  "Includes a prometheus ring middleware using iapetos and bidi.  Bidi's route
  matching is required to remove route params from the time series.

  This library does not include iapetos and bidi as dependencies directly, so
  requiring this namespace will break unless you've done so yourself."
  (:require [bidi.bidi :as bidi]
            [clojure.string :as str]
            [org.purefn.kurosawa.web.auth :as auth]
            [iapetos.core :as prometheus]
            [iapetos.collector.ring :as ring]
            [taoensso.timbre :as log]))

(def metrics-path "/metrics")

(defn path
  [routes ignore-keys {:keys [uri request-method]}]
  (or (->> (bidi/match-route routes uri :request-method request-method)
           (:route-params)
           (remove (comp (set ignore-keys)
                         first))
           (reduce (fn [path [p v]]
                     (str/replace path v (str p)))
                   uri))
      uri))

(defonce registry
  (-> (prometheus/collector-registry)
      (ring/initialize)))

(defn- wrap-basic-auth
  [handler metrics-path basic-auth-opts]
  (fn [{:keys [uri] :as request}]
    (if (= uri metrics-path)
      (handler ((auth/wrap-basic-auth handler basic-auth-opts)
                request))
      (handler request))))

(defn wrap-metrics
  ([handler routes registry]
   (wrap-metrics handler routes registry {}))
  ([handler routes registry {:keys [ignore-keys basic-auth]
                             :as   options}]
   (let [path-fn (partial path routes ignore-keys)]
     (cond-> (ring/wrap-metrics handler registry {:path-fn path-fn
                                                  :path    metrics-path})
             basic-auth
             (wrap-basic-auth metrics-path basic-auth)))))
