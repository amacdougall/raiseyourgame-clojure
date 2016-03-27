# Raise Your Game

Source code for raiseyourga.me, which will eventually be a community annotation
site for Youtube videos. Whether the world needs this project is immaterial; I'm
doing it to practice Clojure and ClojureScript.

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

This application is based on the [Luminus](http://www.luminusweb.net)
scaffolding generator, with the `+postgres`, `+auth`, `+swagger`, and `+cljs`
profiles. Somewhat modified at this point, and will no doubt diverge further.

I suggest starting the REPL with `lein repl`, and then starting and stopping the
webserver with `(start-http-server <port>)` and `(stop-http-server)`.

`lein test` will run the tests, but you can also use `lein test-refresh` to autorun
tests on file changes.

## Running CLJS Tests

The CLJS tests use https://github.com/bensu/doo; follow the instructions in the
Doo readme to set up the Karma test runner. Run the tests under Chrome or
Firefox:

```
lein doo <runner> admin-test [once]
```

The `chrome`, `firefox`, `phantom`, and `slimer` runners are known to be
working; I didn't trouble myself to set up the `node` runner, since the CLJS
will only ever run in the browser anyway.


## License

Copyright © 2016 Alan MacDougall
