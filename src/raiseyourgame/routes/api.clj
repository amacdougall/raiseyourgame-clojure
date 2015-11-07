(ns raiseyourgame.routes.api
  (:require [raiseyourgame.routes.api.users :as users]
            [raiseyourgame.routes.api.videos :as videos]
            [compojure.api.sweet :refer :all]))

(defapi api-routes
  (ring.swagger.ui/swagger-ui
    "/swagger-ui")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
    {:info {:title "Raise Your API"}})

  (context* "/api" []
    users/api-context
    videos/api-context))
