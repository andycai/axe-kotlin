package com.iwayee.activity.dao.mysql

import io.vertx.core.json.JsonObject
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Tuple

object UserDao : MyDao() {
  fun create(user: JsonObject, action: (Long) -> Unit) {
    var fields = "username,password,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    var sql = "INSERT INTO user ($fields) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"

    // 批量插入
    // conn.preparedQuery("INSERT INTO Users (first_name,last_name) VALUES (?, ?)")
    //      .executeBatch(Arrays.asList(
    //        Tuple.of("Julien", "Viet"),
    //        Tuple.of("Emad", "Alblueshi")
    //      ))

    client?.let {
      it.preparedQuery(sql).execute(Tuple.of(
              user.getString("username"),
              user.getString("password"),
              user.getString("token"),
              user.getString("nick"),
              user.getString("wx_token"),
              user.getString("wx_nick"),
              user.getInteger("sex"),
              user.getString("phone"),
              user.getString("email"),
              user.getString("ip"),
              user.getString("activities"),
              user.getString("groups")
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

  fun getUserById(id: Int, action: (JsonObject?) -> Unit) {
    var fields = "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = "SELECT $fields FROM `user` WHERE id = ?"

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

  fun getUserByName(username: String, action: (JsonObject?) -> Unit) {
    var fields = "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = "SELECT $fields FROM `user` WHERE username = ?"

    client?.let {
      it.preparedQuery(sql).execute(Tuple.of(username)) { ar ->
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

  fun getUserByIds(ids: String, action: (JsonObject?) -> Unit) {
    var fields = "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    var sql = "SELECT $fields FROM `user` WHERE id IN($ids)"

    client?.let {
      it.preparedQuery(sql).execute { ar ->
        var jo: JsonObject? = null
        if (ar.succeeded()) {
          var rows = ar.result()
          jo = JsonObject()
          for (row in rows) {
            jo.put(row.getInteger("id").toString(), row.toJson())
          }
        } else {
          println("Failure: ${ar.cause().message}")
        }
        action(jo)
      }
    }
  }

  fun updateUserById(id: Int, user: JsonObject, action: (Boolean) -> Unit) {
    var fields = ("nick = ?, "
            + "wx_nick = ?, "
            + "token = ?, "
            + "wx_token = ?, "
            + "ip = ?, "
            + "groups = ?, "
            + "activities = ?")
    var sql = "UPDATE `user` SET $fields WHERE id = ?"

    client?.let {
      it.preparedQuery(sql).execute(Tuple.of(
              user.getString("nick"),
              user.getString("wx_nick"),
              user.getString("token"),
              user.getString("wx_token"),
              user.getString("ip"),
              user.getJsonArray("groups").encode(),
              user.getJsonArray("activities").encode(),
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
