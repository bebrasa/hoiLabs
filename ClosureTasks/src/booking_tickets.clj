(ns booking-tickets.core)

(def empty-map
  {:forward {}
   :backward {}})

(defn route
  [route-map from to price tickets-num]
  (let [tickets (ref tickets-num :validator (fn [state] (>= state 0)))
        orig-source-desc (or (get-in route-map [:forward from]) {})
        orig-reverse-dest-desc (or (get-in route-map [:backward to]) {})
        route-desc {:price price
                    :tickets tickets}
        source-desc (assoc orig-source-desc to route-desc)
        reverse-dest-desc (assoc orig-reverse-dest-desc from route-desc)]
    (-> route-map
        (assoc-in [:forward from] source-desc)
        (assoc-in [:backward to] reverse-dest-desc))))

(def transact-restarts (atom 0))

(defn- snapshot-graph [route-map]
  (into {}
        (map (fn [[from destinations]]
               [from (mapv (fn [[to {:keys [price tickets]}]]
                             {:from from
                              :to to
                              :price price
                              :tickets tickets
                              :available @tickets})
                           destinations)])
             (:forward route-map))))

(defn- choose-next [frontier]
  (when (seq frontier)
    (apply min-key (comp :cost second) frontier)))

(defn- dijkstra [graph start goal]
  (loop [frontier {start {:cost 0
                          :path [start]
                          :edges []}}
         best {}]
    (if-let [[node {:keys [cost path edges]}] (choose-next frontier)]
      (let [frontier (dissoc frontier node)]
        (cond
          (= node goal)
          {:cost cost
           :path path
           :edges edges}

          (and (contains? best node)
               (<= (best node) cost))
          (recur frontier best)

          :else
          (let [best (assoc best node cost)
                updates (reduce
                          (fn [acc {:keys [to price available] :as edge}]
                            (if (pos? available)
                              (let [new-cost (+ cost price)
                                    existing (acc to)]
                                (if (and existing (<= (:cost existing) new-cost))
                                  acc
                                  (assoc acc to {:cost new-cost
                                                 :path (conj path to)
                                                 :edges (conj edges edge)})))
                              acc))
                          frontier
                          (get graph node []))]
            (recur updates best))))
      nil)))

(defn book-tickets
  [route-map from to]
  (if (= from to)
    {:path '() :price 0}
    (let [local-retries (atom -1)]
      (try
        (dosync
          (swap! local-retries inc)
          (let [graph (snapshot-graph route-map)
                {:keys [cost path edges]} (dijkstra graph from to)]
            (if path
              (do
                (doseq [{:keys [tickets]} edges]
                  (alter tickets dec))
                {:path (apply list path)
                 :price cost})
              {:error :no-tickets})))
        (finally
          (swap! transact-restarts #(+ % (max 0 @local-retries))))))))

(def spec1 (-> empty-map
               (route "City1" "Capital"    200 5)
               (route "Capital" "City1"    250 5)
               (route "City2" "Capital"    200 5)
               (route "Capital" "City2"    250 5)
               (route "City3" "Capital"    300 3)
               (route "Capital" "City3"    400 3)
               (route "City1" "Town1_X"    50 2)
               (route "Town1_X" "City1"    150 2)
               (route "Town1_X" "TownX_2"  50 2)
               (route "TownX_2" "Town1_X"  150 2)
               (route "Town1_X" "TownX_2"  50 2)
               (route "TownX_2" "City2"    50 3)
               (route "City2" "TownX_2"    150 3)
               (route "City2" "Town2_3"    50 2)
               (route "Town2_3" "City2"    150 2)
               (route "Town2_3" "City3"    50 3)
               (route "City3" "Town2_3"    150 2)))

(defn booking-future [route-map from to init-delay loop-delay]
  (future
    (Thread/sleep init-delay)
    (loop [bookings []]
      (Thread/sleep loop-delay)
      (let [booking (book-tickets route-map from to)]
        (if (booking :error)
          bookings
          (recur (conj bookings booking)))))))

(defn print-bookings [name ft]
  (println (str name ":") (count ft) "bookings")
  (doseq [booking ft]
    (println "price:" (booking :price) "path:" (booking :path))))

(defn run []
  (reset! transact-restarts 0)
  (let [f1 (booking-future spec1 "City1" "City3" 0 1)
        f2 (booking-future spec1 "City1" "City2" 100 1)
        f3 (booking-future spec1 "City2" "City3" 100 1)]
    (print-bookings "City1->City3:" @f1)
    (print-bookings "City1->City2:" @f2)
    (print-bookings "City2->City3:" @f3)
    (println "Total (re-)starts:" @transact-restarts)))
