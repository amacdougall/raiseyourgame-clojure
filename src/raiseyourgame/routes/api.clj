(ns raiseyourgame.routes.api
  (:require [raiseyourgame.routes.api.users :refer [users-routes]]
            [raiseyourgame.routes.api.videos :refer [videos-routes]]
            [compojure.api.sweet :refer :all]))

(defapi api-routes
  {:formats [:json-kw :edn :transit-msgpack :transit-json]
   :swagger
   {:ui "/swagger-ui"
    :data {:info {:title "Raise Your API"
                  :description "Transit API docs for raiseyourga.me"}}}}

  (context "/api" []
    (context "/users"  [] users-routes)
    (context "/videos" [] videos-routes)))
