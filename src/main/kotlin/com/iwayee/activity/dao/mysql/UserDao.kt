package com.iwayee.activity.dao.mysql

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

object UserDao : MyDao() {
  private val LOG = LoggerFactory.getLogger(UserDao::class.java)

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
      it.preparedQuery(sql)
              .execute(Tuple.of(
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
              ))
              .onSuccess { rows ->
                val lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID)
                LOG.info("Last Insert Id: $lastInsertId")
                action(lastInsertId)
              }
              .onFailure { th ->
                LOG.info("Failure: ${th.message}")
                action(0)
              }
    }
  }

  fun getUserById(id: Long, action: (JsonObject) -> Unit) {
    var fields = "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = "SELECT $fields FROM `user` WHERE id = ?"

    client?.let {
      it.preparedQuery(sql)
              .execute(Tuple.of(id))
              .onSuccess { rows ->
                var jo = JsonObject()
                for (row in rows) {
                  jo = toJo(row.toJson())
                }
                action(jo)
              }
              .onFailure { th ->
                LOG.info("Failure: ${th.message}")
                action(JsonObject())
              }
    }
  }

  fun getUserByName(username: String, action: (JsonObject) -> Unit) {
    var fields = "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups,create_at";
    var sql = "SELECT $fields FROM `user` WHERE username = ?"

    client?.let {
      it.preparedQuery(sql)
              .execute(Tuple.of(username))
              .onSuccess { rows ->
                var jo = JsonObject()
                for (row in rows) {
                  jo = toJo(row.toJson())
                }
                action(jo)
              }
              .onFailure { th ->
                LOG.info("Failure: ${th.message}")
                action(JsonObject())
              }
    }
  }

  fun getUserByIds(ids: String, action: (JsonObject) -> Unit) {
    var fields = "id,scores,username,token,nick,wx_token,wx_nick,sex,phone,email,ip,activities,groups";
    var sql = "SELECT $fields FROM `user` WHERE id IN($ids)"

    client?.let {
      it.preparedQuery(sql)
              .execute()
              .onSuccess { rows ->
                var jo = JsonObject()
                for (row in rows) {
                  jo.put(row.getInteger("id").toString(), toJo(row.toJson()))
                }
                action(jo)
              }
              .onFailure { th ->
                LOG.info("Failure: ${th.message}")
                action(JsonObject())
              }
    }
  }

  fun updateUserById(id: Long, user: JsonObject, action: (Boolean) -> Unit) {
    var fields = ("nick = ?, "
            + "wx_nick = ?, "
            + "token = ?, "
            + "wx_token = ?, "
            + "ip = ?, "
            + "groups = ?, "
            + "activities = ?")
    var sql = "UPDATE `user` SET $fields WHERE id = ?"

    client?.let {
      it.preparedQuery(sql)
              .execute(Tuple.of(
                      user.getString("nick"),
                      user.getString("wx_nick"),
                      user.getString("token"),
                      user.getString("wx_token"),
                      user.getString("ip"),
                      user.getJsonArray("groups").encode(),
                      user.getJsonArray("activities").encode(),
                      id
              ))
              .onSuccess{
                action(true)
              }
              .onFailure { th ->
                LOG.info("Failure: ${th.message}")
                action(false)
              }
    }
  }

  // 私有方法
  private fun toJo(jo: JsonObject): JsonObject {
    jo.put("groups", JsonArray(jo.getString("groups")))
    jo.put("activities", JsonArray(jo.getString("activities")))
    return jo
  }
}
