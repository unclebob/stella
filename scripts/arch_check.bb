#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[clojure.string :as str])

(def src-root "src")

(defn- read-ns-and-requires [path]
  (let [content (slurp path)
        ns-m (re-find #"\(ns\s+([\w.-]+)" content)
        req-block (re-find #"\(:require\s+([^)]*)\)" content)]
    {:ns (some-> ns-m second symbol)
     :requires (when req-block
                 (->> (str/split (second req-block) #"\s+")
                      (map str/trim)
                      (remove str/blank?)
                      (map #(re-find #"^:?([\w.-]+)" %))
                      (keep second)
                      (map symbol)
                      vec))}))

(defn- src-files []
  (->> (fs/glob src-root "**/*.clj")
       (map str)
       sort))

(defn- forbidden-require? [requires pattern]
  (seq (filter #(re-find pattern (name %)) requires)))

(defn- check-rule [label ns requires pattern]
  (when-let [violations (forbidden-require? requires pattern)]
    {:label label :ns ns :violations violations}))

(def rules
  [{:label "UI and core modules must not depend on CljFX or JavaFX"
    :ns-pattern #"^stella\.(events|actions|ui)"
    :forbidden #"^(cljfx|javafx)"}
   {:label "UI modules must not depend on application or FX adapters"
    :ns-pattern #"^stella\.ui"
    :forbidden #"^stella\.(app|fx)"}
   {:label "Core policy must not depend on UI, app shell, or FX adapters"
    :ns-pattern #"^stella\.actions$"
    :forbidden #"^stella\.(ui|app|fx|cljfx)"}
   {:label "FX effects must not depend on UI descriptions or CljFX"
    :ns-pattern #"^stella\.fx"
    :forbidden #"^(stella\.ui|cljfx)"}])

(defn- violations []
  (mapcat
   (fn [file]
     (let [{:keys [ns requires]} (read-ns-and-requires file)]
       (keep (fn [rule]
               (when (and ns (re-find (:ns-pattern rule) (name ns)))
                 (some-> (check-rule (:label rule) ns requires (:forbidden rule))
                         (assoc :path file))))
             rules)))
   (src-files)))

(let [found (violations)]
  (if (seq found)
    (do
      (println "Architecture check failed:")
      (doseq [{:keys [label ns violations path]} found]
        (println (format "  %s in %s (%s): %s"
                         label ns path (str/join ", " violations))))
      (System/exit 1))
    (do
      (println "Architecture check passed.")
      (System/exit 0))))