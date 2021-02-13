package com.iwayee.activity.dao.mysql

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Tuple

object GroupDao: MyDao() {
  fun create(group: JsonObject, action: (Long) -> Unit) {
    var fields = "`level`,`name`,`members`,`activities`,`pending`,`notice`,`addr`,`logo`";
    var sql = "INSERT INTO `group` ($fields) VALUES (?,?,?,?,?,?,?,?)"
    client?.connection?.onSuccess{ conn ->
      conn.preparedQuery(sql)
        .execute(Tuple.of(
          group.getInteger("level"),
          group.getString("name"),
          group.getJsonArray("members").encode(),
          group.getJsonArray("activities").encode(),
          group.getJsonArray("pending").encode(),
          group.getString("notice"),
          group.getString("addr"),
          group.getString("logo")
        ))
        .onComplete{ ar ->
          if (ar.succeeded()) {
            var rows = ar.result()
            var lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID)
            println("Last Insert Id: $lastInsertId")
            action(lastInsertId)
          } else {
            println("Failure: ${ar.cause().message}")
            action(0L)
          }
          conn.close()
        }
        .onFailure{ ar ->
          action(0L)
          ar.printStackTrace()
        }
    }
  }

  fun getGroupByID(id: Int, action: (JsonObject?) -> Unit) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`,`notice`,`addr`,`activities`";
    var sql = "SELECT $fields FROM `group` WHERE id = ?"
    client?.connection?.onSuccess{ conn ->
      conn.preparedQuery(sql)
        .execute(Tuple.of(id))
        .onComplete{ ar ->
          if (ar.succeeded()) {
            var rows = ar.result()
            if (rows.size() > 0) {
              var jo = JsonObject()
              for (row in rows) {
                jo = row.toJson()
              }
              action(jo)
            } else {
              println("Failure: ${ar.cause().message}")
              action(null)
            }
          }
          conn.close()
        }
        .onFailure{ ar ->
          action(null)
          ar.printStackTrace()
        }
    }
  }

  fun getGroups(page: Int, num: Int, action: (JsonArray) -> Unit) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = "SELECT $fields FROM `group` LIMIT ${(page-1)*num},$num"
    client?.connection?.onSuccess{ conn ->
      conn.preparedQuery(sql)
        .execute()
        .onComplete{ ar ->
          if (ar.succeeded()) {
            var rows = ar.result()
            var jr = JsonArray()
            for (row in rows) {
              var jo = row.toJson()
              jr.add(jo)
            }
            action(jr)
          } else {
            println("Failure: ${ar.cause().message}")
            action(JsonArray())
          }
          conn.close()
        }
        .onFailure{ ar ->
          action(JsonArray())
          ar.printStackTrace()
        }
    }
  }

  fun getGroupsByIds(ids: String, action: (JsonArray) -> Unit) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = "SELECT $fields FROM `group` WHERE id IN($ids)"
    client?.connection?.onSuccess{ conn ->
      conn.preparedQuery(sql)
        .execute()
        .onComplete{ ar ->
          if (ar.succeeded()) {
            var rows = ar.result()
            var jr = JsonArray()
            for (row in rows) {
              var jo = row.toJson()
              jr.add(jo)
            }
            action(jr)
          } else {
            println("Failure: ${ar.cause().message}")
            action(JsonArray())
          }
          conn.close()
        }
        .onFailure{ ar ->
          action(JsonArray())
          ar.printStackTrace()
        }
    }
  }

  fun updateGroupById(id: Int, group: JsonObject, action: (Boolean) -> Unit) {
    val fields = ("level = ?, "
      + "name = ?, "
      + "logo = ?, "
      + "notice = ?, "
      + "addr = ?, "
      + "members = ?, "
      + "pending = ?, "
      + "activities = ?")
    var sql = "UPDATE `group` SET $fields WHERE id = ?"
    client?.connection?.onSuccess{ conn ->
      conn.preparedQuery(sql)
        .execute(Tuple.of(
          group.getInteger("level"),
          group.getString("name"),
          group.getString("logo"),
          group.getString("notice"),
          group.getString("addr"),
          group.getJsonArray("members").encode(),
          group.getJsonArray("pending").encode(),
          group.getJsonArray("activities").encode(),
          id
        ))
        .onComplete{ ar ->
          if (ar.succeeded()) {
            action(true)
          } else {
            println("Failure: ${ar.cause().message}")
            action(false)
          }
          conn.close()
        }
        .onFailure{ ar ->
          action(false)
          ar.printStackTrace()
        }
    }
  }
}
