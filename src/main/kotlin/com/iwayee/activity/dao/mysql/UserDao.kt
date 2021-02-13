package com.iwayee.activity.dao.mysql

import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Tuple

object UserDao: MyDao() {
  fun getUserByID(id: Int, action: (JsonObject?) -> Unit) {
    var fields = "id,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = "SELECT $fields FROM `user` WHERE id = ?"
    client?.connection?.onSuccess{conn ->
      conn.preparedQuery(sql)
        .execute(Tuple.of(id))
        .onComplete{ar ->
          if (ar.succeeded()) {
            var rows = ar.result()
            if (rows.size() > 0) {
              var data = JsonObject()
              for (row in rows) {
                data = row.toJson()
              }
              action(data)
            } else {
              println("Failure: ${ar.cause().message}")
              action(null)
            }
          }
          conn.close()
        }.onFailure{ar ->
          action(null)
          ar.printStackTrace()
        }
    }
  }
}
