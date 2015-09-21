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

## License

Copyright © 2015 Alan MacDougall
