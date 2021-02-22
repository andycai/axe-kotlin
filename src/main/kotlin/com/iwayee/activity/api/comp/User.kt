package com.iwayee.activity.api.comp

import com.iwayee.activity.define.SexType
import io.vertx.core.json.JsonArray

data class User(
        var id: Long = 0,
        var sex: Int = SexType.MALE.ordinal,
        var scores: Int = 0,
        var username: String = "",
        var password: String = "",
        var nick: String = "",
        var wx_nick: String = "",
        var token: String = "",
        var wx_token: String = "",
        var ip: String = "",
        var phone: String = "",
        var email: String = "",
        var create_at: String = "",
        var groups: MutableList<Int> = mutableListOf(),
        var activities: MutableList<Long> = mutableListOf()
) {
  fun containsActivity(aid: Long): Boolean {
    return activities.contains(aid)
  }

  fun addActivity(aid: Long) {
    if (!activities.contains(aid)) {
      activities.add(aid);
    }
  }

  fun removeActivity(aid: Long) {
    if (activities.contains(aid)) {
      activities.remove(aid);
    }
  }

  fun addGroup(gid: Int) {
    if (!groups.contains(gid)) {
      groups.add(gid)
    }
  }

  fun removeGroup(gid: Int) {
    if (groups.contains(gid)) {
      groups.remove(gid)
    }
  }
}
