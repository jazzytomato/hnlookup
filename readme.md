# Hacker News Lookup

![alt tag](https://raw.githubusercontent.com/jazzytomato/hnlookup/master/resources/shared/images/icon128.png)

Hacker News Lookup is a minimal and non-intrusive Chrome extension that allows you to lookup on Hacker News the page that you are currently viewing, and browse for related pages.

### Links

[Chrome Webstore](https://chrome.google.com/webstore/detail/hacker-news-lookup/ekfmfhhfalhmiacchemmhapffjaolffo)

[Blog post](http://jazzytomato.com/hnlookup-chrome-extension-clojurescript/)


### Local setup

#### Extension development

We assume you are familiar with ClojureScript tooling and you have your machine in a good shape running recent versions of
java, maven, leiningen, etc.

  * clone this repo somewhere:

      ```
      git clone https://github.com/jazzytomato/hnlookup.git
      cd hnlookup
      ```
  * it gets built into `resources/unpacked/compiled` folder.

    In one terminal session run:
      ```
      lein dev
      ```
    if you want it to have the hot reload with figwheel, you will need to add the plugin `lein-figwheel` but I use `figwheel-sidecar` within cursive (see instructions below)

  * use latest Chrome Canary with [Custom Formatters](https://github.com/binaryage/cljs-devtools#enable-custom-formatters-in-your-chrome-canary) enabled
  * open Chrome Canary and add `resources/unpacked` via "Load unpacked extension..."


#### Figwheel + Cursive + Live REPL

* add a run configuration under Run/Edit Configurations

* Under clojure REPL, click `+` and select *Use clojure.main in normal JVM process* with parameters "scripts/repl.clj"


### Thanks

To all the authors and contributors of the libraries I use but especially for the [chromex library](https://github.com/binaryage/chromex) and [chromex-sample](https://github.com/binaryage/chromex-sample) boilerplate