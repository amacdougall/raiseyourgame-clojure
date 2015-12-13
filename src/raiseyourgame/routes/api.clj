(ns raiseyourgame.routes.api
  (:require [raiseyourgame.routes.api.users :refer [users-routes]]
            [raiseyourgame.routes.api.videos :refer [videos-routes]]
            [compojure.api.sweet :refer :all]))

(defapi api-routes
  (ring.swagger.ui/swagger-ui
    "/swagger-ui")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
    {:info {:title "Raise Your API"}})

  (context* "/api" []
    (context* "/users" []
      users-routes)
    (context* "/videos" []
      videos-routes)))
