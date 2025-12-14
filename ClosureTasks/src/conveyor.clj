(ns conveyor.core)

(declare supply-msg)
(declare notify-msg)

(defn storage
  [ware notify-step & consumers]
  (let [counter (atom 0 :validator #(>= % 0))
        worker-state {:storage counter
                      :ware ware
                      :notify-step notify-step
                      :consumers consumers}]
    {:storage counter
     :ware ware
     :worker (agent worker-state)}))

(defn factory
  [amount duration target-storage & ware-amounts]
  (let [bill (apply hash-map ware-amounts)
        buffer (reduce-kv (fn [acc k _] (assoc acc k 0))
                          {} bill)
        worker-state {:amount amount
                      :duration duration
                      :target-storage target-storage
                      :bill bill
                      :buffer buffer}]
    {:worker (agent worker-state)}))

(defn source
  [amount duration target-storage]
  (new Thread
       (fn []
         (Thread/sleep duration)
         (send (target-storage :worker) supply-msg amount)
         (recur))))

(defn supply-msg
  [state amount]
  (swap! (state :storage) #(+ % amount))
  (let [ware (state :ware)
        cnt @(state :storage)
        notify-step (state :notify-step)
        consumers (state :consumers)]
    (when (and (> notify-step 0)
               (> (int (/ cnt notify-step))
                  (int (/ (- cnt amount) notify-step))))
      (println (.format (new java.text.SimpleDateFormat "hh.mm.ss.SSS") (new java.util.Date))
               "|" ware "amount: " cnt))
    (when consumers
      (doseq [consumer (shuffle consumers)]
        (send (consumer :worker) notify-msg ware (state :storage) amount))))
  state)

(defn notify-msg
  [state ware storage-atom amount]
  (let [{:keys [bill buffer]} state]
    (if-not (contains? bill ware)
      state
      (let [required (bill ware)
            have (get buffer ware 0)
            missing (max 0 (- required have))
            taken (if (pos? missing)
                    (let [result (atom 0)]
                      (try
                        (swap! storage-atom
                               (fn [current]
                                 (let [take (min missing current)]
                                   (reset! result take)
                                   (- current take))))
                        (catch IllegalStateException _
                          (reset! result 0)))
                      @result)
                    0)
            updated-buffer (if (pos? taken)
                             (update buffer ware + taken)
                             buffer)
            base-state (assoc state :buffer updated-buffer)
            can-run? (fn [buf]
                       (and (seq bill)
                            (every?
                              (fn [[k req]]
                                (>= (get buf k 0) req))
                              bill)))
            run-cycle (fn run-cycle [st]
                        (if (can-run? (:buffer st))
                          (let [{:keys [duration amount target-storage bill]} st
                                _ (Thread/sleep duration)
                                consumed-buffer (reduce-kv
                                                  (fn [buf k req]
                                                    (update buf k (fn [current] (- current req))))
                                                  (:buffer st)
                                                  bill)
                                produced-state (assoc st :buffer consumed-buffer)]
                            (send (target-storage :worker) supply-msg amount)
                            (recur produced-state))
                          st))]
        (run-cycle base-state)))))

(def safe-storage (storage "Safe" 1))
(def safe-factory (factory 1 3000 safe-storage "Metal" 3))
(def cuckoo-clock-storage (storage "Cuckoo-clock" 1))
(def cuckoo-clock-factory (factory 1 2000 cuckoo-clock-storage "Lumber" 5 "Gears" 10))
(def gears-storage (storage "Gears" 20 cuckoo-clock-factory))
(def gears-factory (factory 4 1000 gears-storage "Ore" 4))
(def metal-storage (storage "Metal" 5 safe-factory))
(def metal-factory (factory 1 1000 metal-storage "Ore" 10))
(def lumber-storage (storage "Lumber" 20 cuckoo-clock-factory))
(def lumber-mill (source 5 4000 lumber-storage))
(def ore-storage (storage "Ore" 10 metal-factory gears-factory))
(def ore-mine (source 2 1000 ore-storage))

(defn start []
  (.start ore-mine)
  (.start lumber-mill))

(defn stop []
  (.stop ore-mine)
  (.stop lumber-mill))
