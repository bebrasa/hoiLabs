(ns C3)

(defn partition-lazy
  "Ленивая разбивка последовательности на блоки размера n"
  [n coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (let [chunk (take n s)]
        (cons chunk
              (partition-lazy n (drop n s)))))))

(defn pfilter
  "Параллельная ленивый фильтр с блоками"
  ([pred coll]
   (pfilter pred coll (.availableProcessors (Runtime/getRuntime))))
  ([pred coll parallelism]
   (let [block-size 100
         blocks     (partition-lazy block-size coll)]
     (letfn [(step [active-blocks active-futs]
               (lazy-seq
                 (let [need (- parallelism (count active-futs))
                       new-blocks (take need active-blocks)
                       remaining  (drop need active-blocks)
                       new-futs  (map #(future (doall (filter pred %))) new-blocks)
                       all-futs  (concat active-futs new-futs)]
                   (when (seq all-futs)
                     (lazy-cat
                       @(first all-futs)                ;; берём готовый результат
                       (step remaining (rest all-futs)))))))]
       (step blocks '())))))


(time
  (doall
    (filter
      (fn [x]
        (Thread/sleep 10)
        (even? x))
      (range 1000))))

(time
  (doall
    (pfilter
      (fn [x]
        (Thread/sleep 10)
        (even? x))
      (range 1000))))