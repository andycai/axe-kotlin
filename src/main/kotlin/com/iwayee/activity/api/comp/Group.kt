package com.iwayee.activity.api.comp

import com.iwayee.activity.define.GroupPosition
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class Group(
        var id: Int = 0,
        var level: Int = 1,
        var name: String = "",
        var logo: String = "",
        var notice: String = "",
        var addr: String = "",
        var activities: JsonArray = JsonArray(),
        var members: JsonArray = JsonArray(),
        var pending: JsonArray = JsonArray()
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

  fun isMember(uid: Int): Boolean {
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getInteger("id") == uid) {
        return true
      }
    }
    return false
  }

  fun isOwner(uid: Int): Boolean {
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getInteger("id") == uid
              && jo.getInteger("pos") == GroupPosition.POS_OWNER.ordinal) {
        return true;
      }
    }
    return false
  }

  fun isManager(uid: Int): Boolean {
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getInteger("id") == uid
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

  fun addActivity(aid: Int) {
    if (!activities.contains(aid)) {
      activities.add(aid)
    }
  }

  fun promote(uid: Int): Boolean {
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getInteger("id") == uid) {
        jo.put("pos", GroupPosition.POS_MANAGER.ordinal)
        return true
      }
    }
    return false
  }

  fun transfer(uid: Int, mid: Int): Boolean {
    var b = false
    for (item in members) {
      val jo = item as JsonObject
      if (jo.getInteger("id") == uid) {
        jo.put("pos", GroupPosition.POS_MEMBER.ordinal)
      }
      if (jo.getInteger("id") == mid) {
        jo.put("pos", GroupPosition.POS_OWNER.ordinal)
        b = true
      }
    }
    return b
  }

  fun notIn(uid: Int): Boolean {
    for (item in members) {
      var jo = item as JsonObject
      if (jo.getInteger("id") == uid) {
        return false
      }
    }
    return true
  }
}
