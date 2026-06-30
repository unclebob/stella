(ns stella.qa.args)

(defn parse-qa-flag
  "Extract optional `--qa <seconds>` from argv. Returns `{:qa-seconds n :args [...]}`."
  [args]
  (loop [remaining (seq args)
         qa-seconds nil
         rest-args []]
    (if (empty? remaining)
      {:qa-seconds qa-seconds :args rest-args}
      (cond
        (= "--qa" (first remaining))
        (let [n (Integer/parseInt (second remaining))]
          (recur (drop 2 remaining) n rest-args))
        (= "--debug" (first remaining))
        (recur (rest remaining) qa-seconds rest-args)
        :else
        (recur (rest remaining) qa-seconds (conj rest-args (first remaining)))))))

(defn parse-debug-flag
  "Returns true if `--debug` is present in args."
  [args]
  (boolean (some #(= "--debug" %) args)))

(defn apply-qa-flag!
  [args]
  (let [{:keys [qa-seconds]} (parse-qa-flag args)]
    (when qa-seconds
      (System/setProperty "stella.qa.auto-close-seconds" (str qa-seconds)))))

(defn apply-debug-flag!
  [args]
  (when (parse-debug-flag args)
    (System/setProperty "stella.debug" "true")))

(defn debug? []
  (= "true" (System/getProperty "stella.debug")))

(defn qa-mode? []
  (some? (System/getProperty "stella.qa.auto-close-seconds")))