package com.iwayee.activity.api.comp

import com.iwayee.activity.define.GroupPosition
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class Group(
        var id: Int = 0,
        var scores: Int = 0,
        var level: Int = 1,
        var name: String = "",
        var logo: String = "",
        var notice: String = "",
        var addr: String = "",
        var activities: MutableList<Long> = mutableListOf(),
        var members: JsonArray = JsonArray(),
        var pending: MutableList<Long> = mutableListOf()
) {
  fun toJson(): JsonObject {
    var jo = JsonObject()
    jo.put("id", id)
            .put("level", level)
            .put("logo", logo)
            .put("name", name)
            .put("count", members.size())
    return jo
  }

  fun notInPending(index: Int): Boolean {
    return index < 0 || index >= pending.size
  }

  fun isMember(uid: Long): Boolean {
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getLong("id") == uid) {
        return true
      }
    }
    return false
  }

  fun isOwner(uid: Long): Boolean {
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getLong("id") == uid
              && jo.getInteger("pos") == GroupPosition.POS_OWNER.ordinal) {
        return true;
      }
    }
    return false
  }

  fun isManager(uid: Long): Boolean {
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getLong("id") == uid
              && jo.getInteger("pos") > GroupPosition.POS_MEMBER.ordinal) {
        return true
      }
    }
    return false
  }

  fun managerCount(): Int {
    var count = 0
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getInteger("pos") > GroupPosition.POS_MEMBER.ordinal) {
        count += 1
      }
    }
    return count
  }

  fun addActivity(aid: Long) {
    if (!activities.contains(aid)) {
      activities.add(aid)
    }
  }

  fun promote(uid: Long): Boolean {
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getLong("id") == uid) {
        jo.put("pos", GroupPosition.POS_MANAGER.ordinal)
        return true
      }
    }
    return false
  }

  fun transfer(uid: Long, mid: Long): Boolean {
    var b = false
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getLong("id") == uid) {
        jo.put("pos", GroupPosition.POS_MEMBER.ordinal)
      }
      if (jo.getLong("id") == mid) {
        jo.put("pos", GroupPosition.POS_OWNER.ordinal)
        b = true
      }
    }
    return b
  }

  fun remove(mid: Long): Boolean {
    var it = members.iterator()
    while (it.hasNext()) {
      var jo = it.next() as JsonObject
      if (jo.getLong("id") == mid) {
        it.remove()
        return true
      }
    }
    return false
  }

  fun notIn(uid: Long): Boolean {
    for (item in members) {
      var jo = item as JsonObject
      if (jo.getLong("id") == uid) {
        return false
      }
    }
    return true
  }
}
