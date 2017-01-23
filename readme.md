# hnhit

Based on https://github.com/binaryage/chromex-sample

### Local setup

##### Extension development

We assume you are familiar with ClojureScript tooling and you have your machine in a good shape running recent versions of
java, maven, leiningen, etc.

  * clone this repo somewhere:

      ```
      git clone https://github.com/jazzytomato/hnhit.git
      cd hnhit
      ```
  * chromex sample is gets built into `resources/unpacked/compiled` folder.

    In one terminal session run (will build background and popup pages using figwheel):
      ```
      lein fig
      ```
    In a second terminal session run (will auto-build content-script):
      ```
      lein content
      ```
  * use latest Chrome Canary with [Custom Formatters](https://github.com/binaryage/cljs-devtools#enable-custom-formatters-in-your-chrome-canary) enabled
  * open Chrome Canary and add `resources/unpacked` via "Load unpacked extension..."
  
