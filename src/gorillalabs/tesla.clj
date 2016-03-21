(ns gorillalabs.tesla
  (:require
    [mount.core :as mnt]
    [beckon :as beckon]
    [clojure.tools.logging :as log]
    [environ.core :as env :only [env]]
    [gorillalabs.tesla.component.appstate :as appstate]
    [gorillalabs.tesla.component.configuration :as config]
    [gorillalabs.tesla.component.metering :as metering]
    [gorillalabs.tesla.component.keep-alive :as keep-alive]
    [gorillalabs.tesla.component.handler :as handler]
    [gorillalabs.tesla.component.health :as health]
    [gorillalabs.tesla.component.quartzite :as quartzite]
    [gorillalabs.tesla.component.mongo :as mongo]
    ))

(defn wait! [conf]
  (if-let [wait-time (config/config conf [:wait-ms-on-stop])]
    (try
      (log/info "<- Waiting " wait-time " milliseconds.")
      (Thread/sleep wait-time)
      (catch Exception e (log/error e)))))


(let [default-components
      {:config     #'config/configuration
       :keep-alive #'keep-alive/keep-alive
       :app-status #'appstate/appstate
       :health     #'health/health
       :metering   #'metering/metering
       :handler    #'handler/handler
;       :httpkit    #'httpkit/httpkit
;       :quartzite  #'quartzite/quartzite
;       :mongo      #'mongo/mongo
       }]

  (defn default-components
    ([]
     default-components)
    ([key]
     (default-components key)))


  (defn stop [custom-components]
    (beckon/reinit-all!)
    (log/info "<- System will be stopped. Setting lock.")
    ;   (health/lock-application (:health system))
    (wait! config/configuration)
    (log/info "<- Stopping system.")
    (apply mnt/stop (concat (vals default-components) (vlas custom-components))))

  (defn start [custom-components]
    (log/info "-> Starting system")
    (apply mnt/start (concat (vals default-components) (vals custom-components)))
    (doseq [sig ["INT" "TERM"]]
      (reset! (beckon/signal-atom sig)
              #{(partial apply stop (vals (into {} custom-components)))}))))