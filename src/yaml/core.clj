(ns yaml.core
  ;;(:gen-class)
  )

(defn get-var [line]
  (when-let [[_ commented dashed attrib val]
             (re-find #"^\s*(#*?)\s*(-?)\s*([a-z]\w*):\s*(.*)\n?$" line)]
    {:attrib     attrib
     :dashed     (not= dashed "")
     :val        (when (not= val "") val)
     :commented  (not= commented "")
     :whitespace (count (re-find #"\s*-?\s*" line))}))

(defn extract-vars [text]
  (->> (clojure.string/split text #"\n")
       (map get-var)
       (remove nil?)))

(defn after-nth [ls n]
  (if (= n 0) ls (recur (rest ls) (dec n))))

(defn nest-count [nest]
  (cond
   (or (nil? nest) (empty? nest)) 0
   (map? nest) (+ 1 (nest-count (:nested nest)))
   :else (reduce + (map nest-count nest))))

(defn group-nested-vars
  ([varlist] (group-nested-vars varlist 0))
  ([varlist last-whitespace]
     (if (empty? varlist)
       []
       (let [var        (first varlist)
             whitespace (:whitespace var)]
         (cond
          (< whitespace last-whitespace) []
          (and (nil? (:val var)) (< whitespace (or (:whitespace (second varlist)) 0)))
          (let [nested (group-nested-vars (rest varlist) (:whitespace (second varlist)))]
            (cons (assoc var :nested nested)
                  (group-nested-vars 
                   (after-nth varlist (inc (nest-count nested)))
                   whitespace)))
          :else (cons var (group-nested-vars (rest varlist) whitespace)))))))

(def find-first (comp first (partial remove nil?) filter))

(defn print-var [var]
  (str
   (reduce str (take (- (:whitespace var) (if (:dashed var) 2 0)) (repeat " ")))
   (when (or (:deprecated var) (:commented var)) "# ")
   (when (:dashed var) "- ")
   (:attrib var)
   ": "
   (:val var)
   (when (:deprecated var) " # DEPRECATED")
   "\n"
   (when (:nested var)
     (reduce str (map print-var (:nested var))))))

(defn print-vars [varlist]
  (map print-var varlist))

(declare combine-varlists)

(defn combine-vars [oldvar newvar]
  (if oldvar
    (assoc oldvar
      :nested (seq (combine-varlists (:nested oldvar) (:nested newvar))))
    newvar))

(defn flag-as-deprecated [varlist]
  (map (fn [var]
         (assoc var
           :deprecated true
           :nested (when (:nested var) (flag-as-deprecated (:nested var)))))
       varlist))

(defn combine-varlists [oldvarlist newvarlist]
  (loop [oldvarlist   oldvarlist
         newvarlist   newvarlist
         combinedlist []]
    (if (empty? newvarlist)
      (concat combinedlist (flag-as-deprecated oldvarlist))
      (let [newvar (first newvarlist)
            oldvar (find-first #(= (:attrib %) (:attrib newvar)) oldvarlist)]
        (recur (remove #(= % oldvar) oldvarlist)
               (rest newvarlist)
               (concat combinedlist [(combine-vars oldvar newvar)]))))))

(defn print-combined-yaml [yaml1 yaml2]
  (->>
   (print-vars
    (combine-varlists
     (->>
      (slurp yaml1)
      (extract-vars)
      (group-nested-vars))
     (->>
      (slurp yaml2)
      (extract-vars)
      (group-nested-vars))))
   (reduce str)))

(comment
  (print-combined-yaml "resources/cassandra-1.2.yaml" "resources/cassandra-2.0.yaml")
  )

(defn -main
  [& args]
  (println (print-combined-yaml (first args) (second args)))

  )
