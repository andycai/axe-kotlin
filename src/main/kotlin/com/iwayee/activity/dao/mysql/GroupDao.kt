package com.iwayee.activity.dao.mysql

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

object GroupDao : MyDao() {
  private val LOG = LoggerFactory.getLogger(GroupDao::class.java)

  fun create(group: JsonObject, action: (Long) -> Unit) {
    val fields = "`level`,`name`,`members`,`activities`,`pending`,`notice`,`addr`,`logo`";
    val sql = "INSERT INTO `group` ($fields) VALUES (?,?,?,?,?,?,?,?)"

    client?.let {
      it.preparedQuery(sql)
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

  fun getGroupById(id: Int, action: (JsonObject) -> Unit) {
    val fields = "`id`,`scores`, `level`,`name`,`logo`,`members`, `pending`,`notice`,`addr`,`activities`";
    val sql = "SELECT $fields FROM `group` WHERE id = ?"

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

  fun getGroups(page: Int, num: Int, action: (JsonArray) -> Unit) {
    val fields = "`id`,`scores`, `level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    val sql = "SELECT $fields FROM `group` ORDER BY id DESC LIMIT ${(page - 1) * num},$num"

    client?.let {
      it.preparedQuery(sql)
              .execute()
              .onSuccess { rows ->
                var jr = JsonArray()
                for (row in rows) {
                  jr.add(toJo(row.toJson()))
                }
                action(jr)
              }
              .onFailure { th ->
                LOG.info("Failure: ${th.message}")
                action(JsonArray())
              }
    }
  }

  fun getGroupsByIds(ids: String, action: (JsonArray) -> Unit) {
    val fields = "`id`,`scores`,`level`,`name`,`logo`,`members`, `pending`, `notice`,`addr`,`activities`";
    val sql = "SELECT $fields FROM `group` WHERE id IN($ids)"

    client?.let {
      it.preparedQuery(sql)
              .execute()
              .onSuccess { rows ->
                var jr = JsonArray()
                for (row in rows) {
                  jr.add(toJo(row.toJson()))
                }
                action(jr)
              }
              .onFailure { th ->
                LOG.info("Failure: ${th.message}")
                action(JsonArray())
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
    val sql = "UPDATE `group` SET $fields WHERE id = ?"

    client?.let {
      it.preparedQuery(sql)
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
              .onSuccess {
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
    jo.put("pending", JsonArray(jo.getString("pending")))
    jo.put("activities", JsonArray(jo.getString("activities")))
    return jo
  }
}
