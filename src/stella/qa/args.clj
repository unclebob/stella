(ns stella.qa.args)

(defn parse-qa-flag
  "Extract optional `--qa <seconds>` from argv. Returns `{:qa-seconds n :args [...]}`."
  [args]
  (loop [remaining (seq args)
         qa-seconds nil
         rest-args []]
    (if (empty? remaining)
      {:qa-seconds qa-seconds :args rest-args}
      (if (= "--qa" (first remaining))
        (let [n (Integer/parseInt (second remaining))]
          (recur (drop 2 remaining) n rest-args))
        (recur (rest remaining) qa-seconds (conj rest-args (first remaining)))))))

(defn apply-qa-flag!
  [args]
  (let [{:keys [qa-seconds]} (parse-qa-flag args)]
    (when qa-seconds
      (System/setProperty "stella.qa.auto-close-seconds" (str qa-seconds)))))