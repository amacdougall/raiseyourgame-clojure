Source code for raiseyourga.me.

## Build

### To compile ClojureScript to JavaScript:

```
lein cljsbuild auto dev
```

This launches a process which will recompile the JavaScript output every time a
file in the `cljs-src` folder changes. To compile only once, use `once` instead
of `auto`.

### To run the dev server:

```
lein repl
```

Now you can access the site at [http://localhost:3000](http://localhost:3000).

### To start a browser REPL:

Any page which will host a browser REPL must embed the Austin browser-connected
REPL JS fragment. See resources.clj. In this project, we can just embed it at
the index. Since it's a single-page app, that's all we need.

From the Clojure REPL:
  
```clojure
;; Captures current REPL connection settings in the repl-env Var.
(def repl-env (reset! cemerick.austin.repls/browser-repl-env
                      (cemerick.austin/repl-env)))

;; Enters the local client side of the browser-connected REPL.
(cemerick.austin.repls/cljs-repl repl-env)

;; Finally, reload the page.
```

Now, ClojureScript commands you enter in the REPL in your terminal will be
interpreted by the browser. In a reversal of the natural order, the terminal
REPL can be considered the client, while the browser is the host.

Finally, to execute ClojureScript code in the context of a namespace in the
running app, first use an `ns` directive with _all_ the require statements from
the original code. It's safe to just copy and paste the entire `ns` block from
your source file.
