(ns C2)

(defn primes
  "Ленивое бесконечное решето Эратосфена (без чётных)."
  []
  (letfn [(sieve [nums]
            (lazy-seq
              (let [p (first nums)]
                (cons p
                      (sieve (remove #(zero? (mod % p))
                                     (rest nums)))))))]
    (cons 2 (sieve (iterate #(+ % 2) 3)))))
