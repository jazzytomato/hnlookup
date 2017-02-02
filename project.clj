(defproject binaryage/hnhit "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha12"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"]
                 [binaryage/chromex "0.5.1"]
                 [binaryage/devtools "0.8.2"]
                 [figwheel "0.5.7"]
                 [environ "1.1.0"]
                 [cljs-http "0.1.9"]
                 [reagent "0.6.0"]
                 [re-com "1.3.0"]
                 [cljsjs/moment "2.17.1-0"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.7"]
            [lein-shell "0.5.0"]
            [lein-environ "1.1.0"]
            [lein-cooper "1.2.2"]]

  :source-paths ["src/popup"]

  :clean-targets ^{:protect false} ["target"
                                    "resources/unpacked/compiled"
                                    "resources/release/compiled"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:unpacked
             {:cljsbuild {:builds
                          {:popup
                           {:source-paths ["src/popup"]
                            :figwheel     true
                            :compiler     {:output-to     "resources/unpacked/compiled/popup/main.js"
                                           :output-dir    "resources/unpacked/compiled/popup"
                                           :asset-path    "compiled/popup"
                                           :preloads      [devtools.preload]
                                           :main          hnhit.popup
                                           :optimizations :none
                                           :source-map    true}}}}}

             :checkouts
             ; DON'T FORGET TO UPDATE scripts/ensure-checkouts.sh
             {:cljsbuild {:builds
                          {
                           :popup      {:source-paths ["checkouts/chromex/src/lib"
                                                       "checkouts/chromex/src/exts"]}}}}

             :figwheel
             {:figwheel {:server-port    6888
                         :server-logfile ".figwheel_hnhit.log"
                         :repl           true}}

             :cooper
             {:cooper {"content-dev" ["lein" "content-dev"]
                       "fig-dev"     ["lein" "fig-dev"]
                       "browser"     ["scripts/launch-test-browser.sh"]}}

             :release
             {:env       {:chromex-elide-verbose-logging "true"}
              :cljsbuild {:builds
                          { :popup
                           {:source-paths ["src/popup"]
                            :compiler     {:output-to     "resources/release/compiled/popup.js"
                                           :output-dir    "resources/release/compiled/popup"
                                           :asset-path    "compiled/popup"
                                           :main          hnhit.popup
                                           :optimizations :advanced
                                           :elide-asserts true}}
                           }}}}

  :aliases {"dev-build"   ["with-profile" "+unpacked,+unpacked-content-script,+checkouts,+checkouts-content-script" "cljsbuild" "once"]
            "fig"         ["with-profile" "+unpacked,+figwheel" "figwheel" "popup"]
            "fig-dev"     ["with-profile" "+unpacked,+figwheel,+checkouts" "figwheel" "background" "popup"]
            "devel"       ["with-profile" "+cooper" "do"                                                                      ; for mac only
                           ["shell" "scripts/ensure-checkouts.sh"]
                           ["cooper"]]
            "release"     ["with-profile" "+release" "do"
                           ["clean"]
                           ["cljsbuild" "once" "popup"]]
            "package"     ["shell" "scripts/package.sh"]})
