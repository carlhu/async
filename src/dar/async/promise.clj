(ns dar.async.promise
  (:import (java.lang Exception)))

(defprotocol IPromise
  (fulfill [this val] [this])
  (fulfilled? [this])
  (then [this cb])
  (abort [this])
  (promise-value [this]))

(extend-protocol IPromise
  nil
  (fulfilled? [_] true)
  (then [_ cb] (cb nil))
  (abort [_])
  (promise-value [_] nil)

  java.lang.Object
  (fulfilled? [this] true)
  (then [this cb] (cb this))
  (abort [this])
  (promise-value [this] this))

(defrecord PromiseState [val has-value? callbacks])

(deftype Promise [state abort-cb]
  IPromise
  (fulfill
   [this val]
    (swap! state (fn [state]
                   (when (:has-value? state)
                     (throw (Exception. "Promise was already fulfiled.")))
                   (assoc state :val val :has-value? true)))
   (doseq [cb (:callbacks @state)]
     (cb val)))

   (fulfill
    [this]
    (fulfill this nil))

  (fulfilled?
   [this]
   (:has-value? @state))

  (then
   [this cb]
   (let [state* (swap! state #(if (:has-value? %) %
                                (update-in % [:callbacks] conj cb)))]
     (when (:has-value? state*)
       (cb (:val state*)))))

  (promise-value
   [this]
   (:val @state))

  (abort
   [this]
   (when abort-cb
     (abort-cb))))

(defn make-promise
  ([abort-cb]
   (Promise. (atom (->PromiseState nil false [])) abort-cb))
  ([]
   (make-promise nil)))
