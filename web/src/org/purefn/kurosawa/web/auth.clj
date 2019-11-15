(ns org.purefn.kurosawa.web.auth
  (:require [buddy.auth.middleware :as mw]
            [buddy.auth.backends.httpbasic :as basic-auth]))

(defn wrap-basic-auth
  [handler {:keys [realm authfn] :as opts}]
  (let [auth-backend (basic-auth/http-basic-backend opts)]
    (-> (mw/wrap-authorization handler auth-backend)
        (mw/wrap-authentication auth-backend))))
