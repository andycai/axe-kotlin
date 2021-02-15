package com.iwayee.activity.cache

import com.iwayee.activity.api.comp.Group
import com.iwayee.activity.dao.mysql.GroupDao
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

object GroupCache : BaseCache() {
  private var groups = mutableMapOf<Int, Group>()

  private fun cache(group: Group) {
    group?.let {
      groups[it.id] = it
    }
  }

  fun create(jo: JsonObject, uid: Int, action: (Long) -> Unit) {
    GroupDao.create(jo) {
      if (it > 0L) {
        jo.put("id", it.toInt())
        var group = jo.mapTo(Group::class.java)
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
          return@getGroupById
        }
        action(null)
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
      if (jr.contains(id)) {
        continue
      }
      if (groups.containsKey(id)) {
        groups[id]?.let {
          jr.add(it.toJson())
        }
      } else {
        idsForDB.add(id)
      }
    }

    println("获取群组数据（缓存）：${jr.encode()}")
    if (idsForDB.isEmpty()) {
      action(jr)
      return;
    }

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
    if (groups.containsKey(id)) {
      var group = groups[id]
      GroupDao.updateGroupById(id, JsonObject.mapFrom(group)) {
        action(it)
      }
      return
    }
    action(false)
  }
}
