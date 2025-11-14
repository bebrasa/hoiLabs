(ns C1
  (:require [clojure.string :as str]))

(defn strings-no-repeats [alphabet n]
  (if (<= n 0)
    '("")
    (reduce
      (fn [acc _]
        (mapcat (fn [s]
                  (let [lastch (last s)]
                    (map #(str s %)
                         (remove #{lastch} alphabet))))
                acc))
      alphabet
      (range (dec n)))))

(defn -main [& args]
  (let [[alphabet-arg n-arg] args
        alphabet (->> (str/split (or alphabet-arg "") #",")
                      (remove empty?))
        n (Integer/parseInt (or n-arg "0"))]
    (println (strings-no-repeats alphabet n))))
