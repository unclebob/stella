#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[clojure.string :as str])

(def src-root "src")

(defn- balanced-form [content start]
  (loop [i (inc start) depth 1]
    (if (>= i (count content))
      nil
      (let [c (nth content i)]
        (cond
          (= c \() (recur (inc i) (inc depth))
          (and (= c \)) (= depth 1)) (subs content start (inc i))
          (= c \)) (recur (inc i) (dec depth))
          :else (recur (inc i) depth))))))

(defn- require-block [content]
  (when-let [start (str/index-of content "(:require")]
    (balanced-form content start)))

(defn- read-ns-and-requires [path]
  (let [content (slurp path)
        ns-m (re-find #"\(ns\s+([\w.-]+)" content)
        req-block (require-block content)]
    {:ns (some-> ns-m second symbol)
     :requires (when req-block
                 (->> (str/split req-block #"\s+")
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
    :ns-pattern #"^stella\.(events|actions|model|commands|dispatch|ui)"
    :forbidden #"^(cljfx|javafx)"}
   {:label "UI modules must not depend on application or FX adapters"
    :ns-pattern #"^stella\.ui"
    :forbidden #"^stella\.(app|fx)"}
   {:label "Domain model must not depend on UI, app shell, or FX adapters"
    :ns-pattern #"^stella\.model$"
    :forbidden #"^stella\.(ui|app|fx|cljfx)"}
   {:label "Commands must depend only on the domain model and simulation"
    :ns-pattern #"^stella\.commands$"
    :forbidden #"^stella\.(ui|app|fx|actions|dispatch|cljfx)"}
   {:label "Simulation must depend only on the domain model"
    :ns-pattern #"^stella\.simulation$"
    :forbidden #"^stella\.(ui|app|fx|actions|dispatch|commands|cljfx)"}
   {:label "Thermometer must depend only on the domain model and simulation"
    :ns-pattern #"^stella\.thermometer$"
    :forbidden #"^stella\.(ui|app|fx|actions|dispatch|commands|cljfx)"}
   {:label "UI modules must not depend on simulation"
    :ns-pattern #"^stella\.ui"
    :forbidden #"^stella\.(app|fx|simulation)"}
   {:label "Core policy must not depend on UI, app shell, or FX adapters"
    :ns-pattern #"^stella\.(actions|dispatch)$"
    :forbidden #"^stella\.(ui|app|fx|cljfx)"}
   {:label "Acceptance steps must not depend on live UI or FX adapters"
    :ns-pattern #"^stella\.acceptance"
    :forbidden #"^stella\.(ui|app|fx|cljfx)"}
   {:label "FX adapters must not depend on UI descriptions or CljFX"
    :ns-pattern #"^stella\.fx"
    :forbidden #"^(stella\.ui|cljfx|stella\.app)"}])

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