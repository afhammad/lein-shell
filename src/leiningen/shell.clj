(ns leiningen.shell
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]
            [leiningen.core.utils :as utils]))

(defn- replace-values
  [project s]
  (s/replace s #"\$\{.*?\}"
             (fn [raw]
               ;; Remove first 2 and last character
               (let [val (read-string (subs raw 2 (dec (count raw))))]
                 (if (vector? val)
                   (str (get-in project val))
                   (str (get project val)))))))

(defn- param-expand
  [project s]
  (if-not (string? s)
    s
    (replace-values project s)))

(defmacro ^:private get-setting-fn
  "Returns a function which returns the highest priority setting when called
  with a project and a command. It is a macro because dynamic variables will get
  caught and dereferenced if this was a function. Will return falsey values."
  ([kw] `(get-setting-fn ~kw nil))
  ([kw default]
     `(let [gsym# (gensym "not-found")]
        (fn [project# [command# & args#]]
          (first
           (remove #(= gsym# %)
                   [(get-in project# [:shell :commands command# ~kw] gsym#)
                    (get-in project# [:shell ~kw] gsym#)
                    ~default]))))))

(def ^:private get-environment
  (get-setting-fn :env eval/*env*))

(def ^:private get-directory
  (get-setting-fn :dir eval/*dir*))

(def ^:private get-exit-code
  (get-setting-fn :exit-code :default))

(def ^:private get-pipe-stdin?
  (get-setting-fn :pipe-stdin? true))

(defn- lookup-command
  "Looks up the first part of command, and replaces it with an os-specific
  version if there is one."
  [project cmd]
  (let [command (first cmd)
        os (eval/get-os)]
    (if-let [os-cmd (or (get-in project [:shell :commands command os])
                        (get-in project [:shell :commands command :default-command]))]
      (let [normalized-cmd (if (string? os-cmd) [os-cmd] os-cmd)]
        (main/debug (format "[shell] Replacing command %s with %s. (os is %s)"
                            command normalized-cmd os))
        (concat normalized-cmd (rest cmd)))
      cmd)))

(defn- shell-with-project [project cmd]
  (binding [eval/*dir* (get-directory project cmd)
            eval/*env* (get-environment project cmd)
            eval/*pump-in* (get-pipe-stdin? project cmd)]
    (let [cmd (lookup-command project cmd)]
      (main/debug "[shell] Calling the shell with" cmd)
      (apply eval/sh cmd))))

(defn ^:no-project-needed shell
  "For shelling out from Leiningen. Useful for adding stuff to prep-tasks like
`make` or similar commands, which currently has no Leiningen plugin. If the
process returns a nonzero exit code, this command will force Leiningen to exit
with the same exit code.

Call through `lein shell cmd arg1 arg2 ... arg_n`."
  [project & cmd]
  (let [cmd (mapv #(param-expand project %) cmd)
        exit-code (shell-with-project project cmd)
        exit-code-action (get-exit-code project cmd)]
    (case exit-code-action
      :ignore (main/debug (format "[shell] Ignoring exit code (is %d)"
                                  exit-code))
      :default (if-not (zero? exit-code)
                 (main/exit exit-code)))))
