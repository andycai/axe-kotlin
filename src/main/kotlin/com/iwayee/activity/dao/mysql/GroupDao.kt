package com.iwayee.activity.dao.mysql

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Tuple

object GroupDao : MyDao() {
  fun create(group: JsonObject, action: (Long) -> Unit) {
    var fields = "`level`,`name`,`members`,`activities`,`pending`,`notice`,`addr`,`logo`";
    var sql = "INSERT INTO `group` ($fields) VALUES (?,?,?,?,?,?,?,?)"

    client?.let {
      it.preparedQuery(sql).execute(Tuple.of(
              group.getInteger("level"),
              group.getString("name"),
              group.getJsonArray("members").encode(),
              group.getJsonArray("activities").encode(),
              group.getJsonArray("pending").encode(),
              group.getString("notice"),
              group.getString("addr"),
              group.getString("logo")
      )) { ar ->
        var lastInsertId = 0L
        if (ar.succeeded()) {
          var rows = ar.result()
          lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID)
          println("Last Insert Id: $lastInsertId")
        } else {
          println("Failure: ${ar.cause().message}")
        }
        action(lastInsertId)
      }
    }
  }

  fun getGroupByID(id: Int, action: (JsonObject?) -> Unit) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`,`notice`,`addr`,`activities`";
    var sql = "SELECT $fields FROM `group` WHERE id = ?"

    client?.let {
      it.preparedQuery(sql).execute(Tuple.of(id)) { ar ->
        var jo: JsonObject? = null
        if (ar.succeeded()) {
          var rows = ar.result()
          for (row in rows) {
            jo = row.toJson()
          }
        } else {
          println("Failure: ${ar.cause().message}")
        }
        action(jo)
      }
    }
  }

  fun getGroups(page: Int, num: Int, action: (JsonArray) -> Unit) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = "SELECT $fields FROM `group` ORDER BY id DESC LIMIT ${(page - 1) * num},$num"

    client?.let {
      it.preparedQuery(sql).execute { ar ->
        var jr = JsonArray()
        if (ar.succeeded()) {
          var rows = ar.result()
          for (row in rows) {
            jr.add(row.toJson())
          }
        } else {
          println("Failure: ${ar.cause().message}")
        }
        action(jr)
      }
    }
  }

  fun getGroupsByIds(ids: String, action: (JsonArray) -> Unit) {
    var fields = "`id`, `level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    var sql = "SELECT $fields FROM `group` WHERE id IN($ids)"

    client?.let {
      it.preparedQuery(sql).execute { ar ->
        var jr = JsonArray()
        if (ar.succeeded()) {
          var rows = ar.result()
          for (row in rows) {
            jr.add(row.toJson())
          }
        } else {
          println("Failure: ${ar.cause().message}")
        }
        action(jr)
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

    client?.let {
      it.preparedQuery(sql).execute(Tuple.of(
              group.getInteger("level"),
              group.getString("name"),
              group.getString("logo"),
              group.getString("notice"),
              group.getString("addr"),
              group.getJsonArray("members").encode(),
              group.getJsonArray("pending").encode(),
              group.getJsonArray("activities").encode(),
              id
      )) { ar ->
        var ret = false
        if (ar.succeeded()) {
          ret = true
        } else {
          println("Failure: ${ar.cause().message}")
        }
        action(ret)
      }
    }
  }
}
