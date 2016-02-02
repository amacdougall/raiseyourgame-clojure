(ns raiseyourgame.db.core
  (:require
    [clojure.java.jdbc :as jdbc]
    [taoclj.foundation :as pg]
    [environ.core :refer [env]]))

; Stub
(defn connect! [] nil)

; Stub
(defn disconnect! [] nil)

; this macro creates a def, so we can refer to it elsewhere
(pg/def-datasource conn (env :datasource))

(pg/def-query create-user! {:file "sql/users/create_user.sql"})
(pg/def-query get-users {:file "sql/users/get_users.sql"})
(pg/def-query find-users-by-user-id {:file "sql/users/find_users_by_user_id.sql"})
(pg/def-query find-users-by-email {:file "sql/users/find_users_by_email.sql"})
(pg/def-query find-users-by-username {:file "sql/users/find_users_by_username.sql"})
(pg/def-query update-user! {:file "sql/users/update_user.sql"})

(pg/def-query create-video! {:file "sql/videos/create_video.sql"})
(pg/def-query get-videos {:file  "sql/videos/get_videos.sql"})
(pg/def-query find-videos-by-video-id {:file  "sql/videos/find_videos_by_video_id.sql"})
(pg/def-query find-videos-by-user-id {:file  "sql/videos/find_videos_by_user_id.sql"})
(pg/def-query update-video! {:file  "sql/videos/update_video.sql"})
(pg/def-query delete-video! {:file  "sql/videos/delete_video.sql"})
