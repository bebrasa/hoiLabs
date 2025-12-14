(ns philosophers.dining
  (:require [clojure.string :as str]))

(defn- fork
  [id]
  (ref {:id id
        :uses 0
        :taken false
        :last-owner nil}))

(defn- order-forks
  [strategy idx left right]
  (case strategy
    :ordered (if (< (System/identityHashCode left)
                    (System/identityHashCode right))
               [left right]
               [right left])
    :left-right [left right]
    :right-left [right left]
    :alternating (if (even? idx) [left right] [right left])
    :random (if (zero? (rand-int 2)) [left right] [right left])
    ;; default: stable ordering by identity hash code
    (if (< (System/identityHashCode left)
           (System/identityHashCode right))
      [left right]
      [right left])))

(defn- acquire-forks!
  [{:keys [strategy restart-counter]} idx left right]
  (loop []
    (let [outcome
          (dosync
            (let [[first second] (order-forks strategy idx left right)]
              (if (or (:taken @first)
                      (:taken @second))
                :retry
                (do
                  (alter first assoc :taken true :last-owner idx)
                  (alter second assoc :taken true :last-owner idx)
                  :acquired))))]
      (if (= :acquired outcome)
        :acquired
        (do
          (swap! restart-counter inc)
          (Thread/yield)
          (recur))))))

(defn- release-forks!
  [left right]
  (dosync
    (alter left (fn [state]
                  (-> state
                      (assoc :taken false)
                      (update :uses inc))))
    (alter right (fn [state]
                   (-> state
                       (assoc :taken false)
                       (update :uses inc))))))

(defn- philosopher-loop
  [{:keys [idx iterations think-ms eat-ms forks restart-counter strategy]}]
  (let [count-forks (count forks)
        left (forks idx)
        right (forks (mod (inc idx) count-forks))]
    (loop [remaining iterations
           meals 0]
      (if (zero? remaining)
        {:philosopher idx
         :meals meals}
        (do
          (when (pos? think-ms)
            (Thread/sleep think-ms))
          (acquire-forks! {:strategy strategy
                           :restart-counter restart-counter}
                          idx left right)
          (when (pos? eat-ms)
            (Thread/sleep eat-ms))
          (release-forks! left right)
          (recur (dec remaining) (inc meals)))))))

(defn simulation
  "Runs dining philosophers simulation.
   Options:
     :philosophers – number of philosophers (default 5)
     :iterations – number of meals per philosopher (default 10)
     :think-ms – think duration in ms (default 5)
     :eat-ms – dining duration in ms (default 5)
     :strategy – :ordered (default), :left-right, :right-left, :alternating, :random
     :warmup-ms – optional pause before starting (default 0)
     :timeout-ms – optional timeout for execution (default nil)
   Returns map with stats."
  [{:keys [philosophers iterations think-ms eat-ms strategy warmup-ms timeout-ms]
    :or {philosophers 5
         iterations 10
         think-ms 5
         eat-ms 5
         strategy :ordered
         warmup-ms 0}}]
  (let [forks (vec (map fork (range philosophers)))
        restart-counter (atom 0)
        start-latch (promise)
        workers (mapv (fn [idx]
                        (future
                          @start-latch
                          (philosopher-loop {:idx idx
                                             :iterations iterations
                                             :think-ms think-ms
                                             :eat-ms eat-ms
                                             :forks forks
                                             :restart-counter restart-counter
                                             :strategy strategy})))
                      (range philosophers))
        start-time (System/nanoTime)]
    (when (pos? warmup-ms)
      (Thread/sleep warmup-ms))
    (deliver start-latch true)
    (let [deadline (when timeout-ms (+ (System/currentTimeMillis) timeout-ms))
          results (loop [remaining workers
                         acc []]
                    (if (empty? remaining)
                      acc
                      (let [worker (first remaining)
                            time-left (when deadline (max 1 (- deadline (System/currentTimeMillis))))
                            value (if timeout-ms
                                    (deref worker time-left ::timeout)
                                    (deref worker))]
                        (if (= value ::timeout)
                          (do
                            (doseq [w remaining] (future-cancel w))
                            ::timeout)
                          (recur (rest remaining) (conj acc value))))))
          duration-ms (/ (- (System/nanoTime) start-time) 1e6)
          fork-usage (mapv (fn [ref] (:uses @ref)) forks)]
      (if (= results ::timeout)
        {:status :timeout
         :duration-ms duration-ms
         :restart-count @restart-counter
         :fork-usage fork-usage
         :philosophers []
         :config {:philosophers philosophers
                  :iterations iterations
                  :think-ms think-ms
                  :eat-ms eat-ms
                  :strategy strategy
                  :timeout-ms timeout-ms}}
        (let [meal-results (vec results)
              total-meals (reduce + (map :meals meal-results))]
          {:status :completed
           :duration-ms duration-ms
           :restart-count @restart-counter
           :fork-usage fork-usage
           :philosophers meal-results
           :total-meals total-meals
           :config {:philosophers philosophers
                    :iterations iterations
                    :think-ms think-ms
                    :eat-ms eat-ms
                    :strategy strategy
                    :timeout-ms timeout-ms}})))))

(defn summary
  [{:keys [status duration-ms restart-count fork-usage philosophers config]}]
  (str/join
    \newline
    [(format "Status: %s" (name status))
     (format "Duration: %.2f ms" duration-ms)
     (format "Transaction restarts: %d" restart-count)
     (format "Fork usage: %s" (pr-str fork-usage))
     (format "Meals per philosopher: %s"
             (pr-str (mapv :meals philosophers)))
     (format "Config: %s" (pr-str config))]))

(defn demo []
  (let [configs [{:label "Odd philosophers (fair ordering)"
                  :opts {:philosophers 5 :iterations 20 :think-ms 2 :eat-ms 2 :strategy :ordered}}
                 {:label "Even philosophers (alternating strategy)"
                  :opts {:philosophers 6 :iterations 20 :think-ms 2 :eat-ms 2 :strategy :alternating}}
                 {:label "Even philosophers (contended, potential live-lock)"
                  :opts {:philosophers 6 :iterations 200 :think-ms 0 :eat-ms 2 :strategy :left-right :timeout-ms 200}}]]
    (doseq [{:keys [label opts]} configs]
      (println "== " label)
      (println (summary (simulation opts)))
      (println))))
