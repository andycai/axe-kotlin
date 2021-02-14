package com.iwayee.activity.dao.mysql

import io.vertx.core.json.JsonObject
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Tuple

object UserDao: MyDao() {
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
      )){ ar ->
        if (ar.succeeded()) {
          var rows = ar.result()
          var lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID)
          println("Last Insert Id: $lastInsertId")
          action(lastInsertId)
        } else {
          println("Failure: ${ar.cause().message}")
          action(0L)
        }
      }
    }
  }

  fun getUserByID(id: Int, action: (JsonObject?) -> Unit) {
    var fields = "id,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = "SELECT $fields FROM `user` WHERE id = ?"

    client?.let {
      it.preparedQuery(sql).execute(Tuple.of(id)) { ar ->
        if (ar.succeeded()) {
          var rows = ar.result()
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
    }
  }

  fun getUserByName(username: String, action: (JsonObject?) -> Unit) {
    var fields = "id,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = "SELECT $fields FROM `user` WHERE username = ?"

    client?.let {
      it.preparedQuery(sql).execute(Tuple.of(username)) { ar ->
        if (ar.succeeded()) {
          var rows = ar.result()
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
    }
  }

  fun getUserByIds(ids: String, action: (JsonObject?) -> Unit) {
    var fields = "id,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    var sql = "SELECT $fields FROM `user` WHERE id IN($ids)"

    client?.let {
      it.preparedQuery(sql).execute{ ar ->
        if (ar.succeeded()) {
          var rows = ar.result()
          if (rows.size() > 0) {
            var jo = JsonObject()
            for (row in rows) {
              jo.put(row.getInteger("id").toString(), row.toJson())
            }
            action(jo)
          } else {
            println("Failure: ${ar.cause().message}")
            action(JsonObject())
          }
        }
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
        if (ar.succeeded()) {
          action(true)
        } else {
          println("Failure: ${ar.cause().message}")
          action(false)
        }
      }
    }
  }
}
