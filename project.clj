(defproject binaryage/hnlookup "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha12"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"]
                 [binaryage/chromex "0.5.1"]
                 [binaryage/devtools "0.8.2"]
                 [figwheel "0.5.9"]
                 [environ "1.1.0"]
                 [cljs-http "0.1.9"]
                 [reagent "0.6.0"]
                 [re-com "1.3.0"]
                 [cljsjs/moment "2.17.1-0"]
                 [figwheel-sidecar "0.5.9"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-shell "0.5.0"]
            [lein-environ "1.1.0"]]

  :source-paths ["src/popup" "scripts"]

  :clean-targets ^{:protect false} ["target"
                                    "resources/unpacked/compiled"
                                    "resources/release/compiled"]

  :figwheel {:server-port    6888
              :server-logfile ".figwheel_hnlookup.log"
              :repl           true}

  :cljsbuild  {:builds
                {:dev
                 {:source-paths ["src/popup"]
                  :figwheel     true
                  :compiler     {:output-to     "resources/unpacked/compiled/popup/main.js"
                                 :output-dir    "resources/unpacked/compiled/popup"
                                 :asset-path    "compiled/popup"
                                 :preloads      [devtools.preload]
                                 :main          hnlookup.popup
                                 :optimizations :none
                                 :source-map    true}}
               :release
                {:source-paths ["src/popup"]
                 :compiler     {:output-to     "resources/release/compiled/popup.js"
                                :output-dir    "resources/release/compiled/popup"
                                :asset-path    "compiled/popup"
                                :main          hnlookup.popup
                                :optimizations :advanced
                                :elide-asserts true}}}}


  :aliases {"dev"         ["cljsbuild" "auto" "dev"]
            "release"     ["do"
                           ["clean"]
                           ["cljsbuild" "once" "release"]]
            "package"     ["shell" "scripts/package.sh"]})
