package com.iwayee.activity.dao.mysql

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

object ActivityDao : MyDao() {
  private val LOG = LoggerFactory.getLogger(ActivityDao::class.java)

  fun create(act: JsonObject, action: (Long) -> Unit) {
    var fields = "planner,group_id,kind,type,quota,title,`remark`,status,fee_type,fee_male,fee_female,queue,queue_sex,addr,ahead,begin_at,end_at";
    var sql = "INSERT INTO `activity` ($fields) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

    client?.let {
      it.preparedQuery(sql)
              .execute(Tuple.of(
                      act.getInteger("planner"),
                      act.getInteger("group_id"),
                      act.getInteger("kind"),
                      act.getInteger("type"),
                      act.getInteger("quota"),
                      act.getString("title"),
                      act.getString("remark"),
                      act.getInteger("status"),
                      act.getInteger("fee_type"),
                      act.getInteger("fee_male"),
                      act.getInteger("fee_female"),
                      act.getString("queue"),
                      act.getString("queue_sex"),
                      act.getString("addr"),
                      act.getInteger("ahead"),
                      act.getString("begin_at"),
                      act.getString("end_at")
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

  fun getActivityById(id: Long, action: (JsonObject) -> Unit) {
    var fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    var sql = "SELECT $fields FROM `activity` WHERE id = ?"

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

  fun getActivitiesByType(type: Int, status: Int, page: Int, num: Int, action: (JsonArray) -> Unit) {
    var fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`,`queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`";
    var sql = "SELECT $fields FROM `activity` WHERE `type` = ? AND `status` = ? ORDER BY `id` DESC LIMIT ${(page - 1) * num},$num"

    client?.let {
      it.preparedQuery(sql)
              .execute(Tuple.of(
                      type,
                      status
              ))
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

  fun getActivitiesByIds(ids: String, action: (JsonArray) -> Unit) {
    val fields = "`id`,`planner`,`group_id`,`kind`,`type`,`quota`,`title`,`remark`,`status`,`fee_type`,`fee_male`,`fee_female`,`queue`, `queue_sex`,`addr`,`ahead`,`begin_at`,`end_at`"
    var sql = "SELECT $fields FROM `activity` WHERE id IN($ids)"

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

  fun updateActivityById(id: Long, activity: JsonObject, action: (Boolean) -> Unit) {
    val fields = ("quota = ?, "
            + "title = ?, "
            + "remark = ?, "
            + "status = ?, "
            + "ahead = ?, "
            + "queue = ?, "
            + "queue_sex = ?, "
            + "fee_male = ?, "
            + "fee_female = ?, "
            + "begin_at = ?, "
            + "end_at = ?, "
            + "addr = ?")
    var sql = "UPDATE `activity` SET $fields WHERE `id` = ?"

    client?.let {
      it.preparedQuery(sql)
              .execute(Tuple.of(
                      activity.getInteger("quota"),
                      activity.getString("title"),
                      activity.getString("remark"),
                      activity.getInteger("status"),
                      activity.getInteger("ahead"),
                      activity.getJsonArray("queue").encode(),
                      activity.getJsonArray("queue_sex").encode(),
                      activity.getInteger("fee_male"),
                      activity.getInteger("fee_female"),
                      activity.getString("begin_at"),
                      activity.getString("end_at"),
                      activity.getString("addr"),
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

  fun updateActivityStatus(id: Long, activity: JsonObject, action: (Boolean) -> Unit) {
    val fields = (""
            + "status = ?, "
            + "fee_male = ?, "
            + "fee_female = ?"
            )
    var sql = "UPDATE `activity` SET $fields WHERE `id` = ?"

    client?.let {
      it.preparedQuery(sql)
              .execute(Tuple.of(
                      activity.getInteger("status"),
                      activity.getInteger("fee_male"),
                      activity.getInteger("fee_female"),
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
    jo.put("queue", JsonArray(jo.getString("queue")))
    jo.put("queue_sex", JsonArray(jo.getString("queue_sex")))
    return jo
  }
}
