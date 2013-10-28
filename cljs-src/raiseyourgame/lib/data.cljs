(ns raiseyourgame.lib.data
  (:require [goog.net.XhrIo :as xhr]
            [cljs.reader]
            [cljs.core.async :refer [>! <! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def media-types
  "Available media types and associated decode methods, usable in data calls."
  {:edn ["application/edn" #(cljs.reader/read-string %)]
   :json ["application/json" #(.parse js/JSON %)]})

(defn GET
  "GET [url media-type raw]

  Initiates an AJAX request for the data at the supplied url, immediately
  returning an output channel which will receive the data.

  The media-type param is optional, defaulting to :edn; it may be any of the
  media types defined in raiseyourgame.lib.data/media-types.

  The raw param is optional, defaulting to false. If true, the raw string
  response will be placed in the output channel; otherwise, the response
  will be converted to an appropriate data respresentation.

  In most situations, it will be sufficient to supply a media-type, if
  anything.

  Examples:

  (let [out (GET \"http://api.example.com/path/to/edn/data\")]
    (go (handle-clojure-data (<! out))))
  ; => value taken from out is pure ClojureScript data read in from EDN

  (let [out (GET \"http://api.example.com/path/to/json/data\" :json)]
    (go (handle-js-object (<! out))))
  ; => value taken from out is a JavaScript object

  (let [out (GET \"http://api.example.com/path/to/json/data\" :json :raw)]
    (go (handle-raw-string (<! out))))
  ; => value taken from out is a raw EDN string"
  ([url]
   (GET url :edn))
  ([url media-type]
   (GET url media-type false))
  ([url media-type raw]
   (let [[mime-type decode] (media-type media-types)
         decode (if raw
                  identity ; just pass the string through
                  decode)
         out (chan 1)]
     (xhr/send url
               (fn [event]
                 (let [response (-> event .-target .getResponseText)]
                   (go (>! out (decode response))
                       (close! out))))
               "GET"
               nil ; no extra content
               (clj->js {"Accept" mime-type})) ; headers
     out)))
