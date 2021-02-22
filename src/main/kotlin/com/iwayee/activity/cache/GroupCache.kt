package com.iwayee.activity.cache

import com.iwayee.activity.api.comp.Group
import com.iwayee.activity.dao.mysql.GroupDao
import com.iwayee.activity.define.GroupPosition
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.*

object GroupCache : BaseCache() {
  private var groups = mutableMapOf<Int, Group>()

  fun create(jo: JsonObject, uid: Long, action: (Long) -> Unit) {
    var group = jo.mapTo(Group::class.java)
    var now = Date().time
    var member = JsonObject()
    member.put("id", uid)
            .put("scores", 0)
            .put("pos", GroupPosition.POS_OWNER.ordinal)
            .put("at", now)
    group.members = JsonArray().add(member)
    group.pending = mutableListOf()
    group.activities = mutableListOf()
    GroupDao.create(JsonObject.mapFrom(group)) {
      if (it > 0L) {
        group.id = it.toInt()
        cache(group)
      }
      action(it)
    }
  }

  fun getGroupById(id: Int, action: (Group?) -> Unit) {
    if (groups.containsKey(id)) {
      println("获取群组数据（缓存）: $id")
      action(groups[id])
    } else {
      println("获取群组数据（DB)：$id")
      GroupDao.getGroupById(id) {
        it?.let {
          var group = it.mapTo(Group::class.java)
          cache(group)
          action(group)
        }?: action(null)
      }
    }
  }

  fun getGroupsByIds(ids: List<Int>, action: (JsonArray) -> Unit) {
    var jr = JsonArray()
    var idsForDB = mutableListOf<Int>()
    if (ids.isEmpty()) {
      action(jr)
      return
    }

    for (id in ids) {
      when {
        jr.contains(id) -> continue
        groups.containsKey(id) -> {
          groups[id]?.let {
            jr.add(it.toJson())
          }
        }
        else -> idsForDB.add(id)
      }
    }

    println("获取群组数据（缓存）：${jr.encode()}")
    when {
      idsForDB.isEmpty() -> action(jr)
      else -> {
        var idStr = joiner.join(idsForDB)
        println("获取群组数据（DB）：$idStr")
        GroupDao.getGroupsByIds(idStr) {
          it?.forEach { entry ->
            var jo = entry as JsonObject
            var group = jo.mapTo(Group::class.java)
            cache(group)
            jr.add(group.toJson())
          }
          action(jr)
        }
      }
    }
  }

  fun getGroups(page: Int, num: Int, action: (JsonArray) -> Unit) {
    // 缓存60秒
    GroupDao.getGroups(page, num) {
      var jr = JsonArray()
      for (g in it) {
        var group = (g as JsonObject).mapTo(Group::class.java)
        cache(group)
        groups[group.id] = group

        jr.add(group.toJson())
      }
      action(jr)
    }
  }

  fun syncToDB(id: Int, action: (Boolean) -> Unit) {
    when {
      groups.containsKey(id) -> {
        var group = groups[id]
        GroupDao.updateGroupById(id, JsonObject.mapFrom(group)) {
          action(it)
        }
      }
      else -> action(false)
    }
  }

  // 私有方法
  private fun cache(group: Group) {
    group?.let {
      groups[it.id] = it
    }
  }
}
