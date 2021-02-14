package com.iwayee.activity.cache

import com.iwayee.activity.api.comp.Member
import com.iwayee.activity.api.comp.Player
import com.iwayee.activity.api.comp.Session
import com.iwayee.activity.api.comp.User
import com.iwayee.activity.dao.mysql.UserDao
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.*

object UserCache : BaseCache() {
  private var usersForName = mutableMapOf<String, User>()
  private var usersForId = mutableMapOf<Int, User>()
  private var sessions = mutableMapOf<String, Session>()

  fun cacheSession(token: String, uid: Int, sex: Int) {
    var session: Session? = null
    if (sessions.containsKey(token)) {
      session = sessions[token]
    } else {
      session = Session(uid)
      sessions[token] = session
    }
    session?.let {
      it.token = token
      it.uid = uid
      it.sex = sex
      it.at = Date().time
    }
  }

  fun clearSession(token: String) {
    if (sessions.containsKey(token)) {
      sessions.remove(token)
    }
  }

  fun currentId(token: String): Int {
    var s = sessions[token]
    if (s != null) {
      return s.uid
    }
    return 0
  }

  fun currentSex(token: String): Int {
    var s = sessions[token]
    if (s != null) {
      return s.sex
    }
    return 0
  }

  fun expired(token: String): Boolean {
    var s = sessions[token]
    if (s != null) {
      var now = Date().time
      return (now - s.at) > (2 * 24 * 60 * 60 * 1000)
    }
    return true
  }

  private fun cacheUser(user: User) {
    usersForId[user.id] = user
    usersForName[user.username] = user
  }

  fun toPlayer(usersMap: Map<Int, User>): JsonObject {
    var jo = JsonObject()
    usersMap?.forEach { (key, value) ->
      var player = Player()
      player.fromUser(value)
      jo.put(key.toString(), player.toJson())
    }
    return jo
  }

  fun toMember(usersMap: Map<Int, User>, members: JsonArray): JsonArray {
    var jr = JsonArray()
    for (item in members) {
      var mb = (item as JsonObject).mapTo(Member::class.java)
      usersMap[mb.id]?.let {
        mb.fromUser(it)
      }
      jr.add(JsonObject.mapFrom(mb))
    }
    return jr
  }

  fun create(jo: JsonObject, action: (Long) -> Unit) {
    UserDao.create(jo) {
      if (it > 0L) {
        jo.put("id", it.toInt())
        var user = jo.mapTo(User::class.java)
        cacheUser(user)
      }
      action(it)
    }
  }

  fun getUserByName(name: String, action: (User?) -> Unit) {
    if (usersForName.containsKey(name)) {
      println("获取用户数据（缓存）: $name")
      usersForName[name]?.let {
        action(it)
      }
    } else {
      println("获取用户数据（DB)：$name")
      UserDao.getUserByName(name) {
        it?.let {
          var user = it.mapTo(User::class.java)
          cacheUser(user)
        }
        action(usersForName[name])
      }
    }
  }

  fun getUserById(id: Int, action: (User?) -> Unit) {
    if (usersForId.containsKey(id)) {
      println("获取用户数据（缓存）: $id")
      action(usersForId[id])
    } else {
      println("获取用户数据（DB)：$id")
      UserDao.getUserById(id) {
        it?.let {
          var user = it.mapTo(User::class.java)
          cacheUser(user)
          action(user)
          return@getUserById
        }
        action(null)
      }
    }
  }

  fun getUsersByIds(ids: List<Int>, action: (Map<Int, User>) -> Unit) {
    var itemMap = mutableMapOf<Int, User>()
    var idsForDB = mutableListOf<Int>()
    if (ids.isEmpty()) {
      action(itemMap)
      return
    }

    for (id in ids) {
      if (itemMap.containsKey(id)) {
        continue
      }
      if (usersForId.containsKey(id)) {
        usersForId[id]?.let {
          itemMap[id] = it
        }
      } else {
        idsForDB.add(id)
      }
    }

    println("获取用户数据（缓存）：$itemMap")
    if (idsForDB.isEmpty()) {
      action(itemMap)
      return;
    }

    var idStr = joiner.join(idsForDB)
    println("获取用户数据（DB）：$idStr")
    UserDao.getUserByIds(idStr) {
      it?.forEach { entry ->
        var jo = entry.value as JsonObject
        var user = jo.mapTo(User::class.java)
        cacheUser(user)
        itemMap[user.id] = user
      }
      action(itemMap)
    }
  }

  fun syncToDB(id: Int, action: (Boolean) -> Unit) {
    if (usersForId.containsKey(id)) {
      var user = usersForId[id]
      UserDao.updateUserById(id, JsonObject.mapFrom(user)) {
        action(it)
      }
    }
    action(false)
  }
}
