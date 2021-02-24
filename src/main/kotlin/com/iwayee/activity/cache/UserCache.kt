package com.iwayee.activity.cache

import com.iwayee.activity.api.comp.Member
import com.iwayee.activity.api.comp.Player
import com.iwayee.activity.api.comp.Session
import com.iwayee.activity.api.comp.User
import com.iwayee.activity.dao.mysql.UserDao
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.*

object UserCache : BaseCache() {
  private var usersForName = mutableMapOf<String, User>()
  private var usersForId = mutableMapOf<Long, User>()
  private var sessions = mutableMapOf<String, Session>()
  private val LOG = LoggerFactory.getLogger(UserCache::class.java)

  fun cacheSession(token: String, uid: Long, sex: Int) {
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

  fun currentId(token: String): Long {
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

  fun toPlayer(usersMap: Map<Long, User>): JsonObject {
    var jo = JsonObject()
    usersMap?.forEach { (key, value) ->
      var player = Player()
      player.fromUser(value)
      jo.put(key.toString(), player.toJson())
    }
    return jo
  }

  fun toMember(usersMap: Map<Long, User>, members: JsonArray): JsonArray {
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
        jo.put("id", it)
        var user = jo.mapTo(User::class.java)
        cacheUser(user)
      }
      action(it)
    }
  }

  fun getUserByName(name: String, action: (User?) -> Unit) {
    if (usersForName.containsKey(name)) {
      LOG.info("获取用户数据（缓存）: $name")
      usersForName[name]?.let {
        action(it)
      }
    } else {
      LOG.info("获取用户数据（DB)：$name")
      UserDao.getUserByName(name) {
        it?.let {
          var user = it.mapTo(User::class.java)
          cacheUser(user)
          action(user)
        }?: action(null)
      }
    }
  }

  fun getUserById(id: Long, action: (User?) -> Unit) {
    if (usersForId.containsKey(id)) {
      LOG.info("获取用户数据（缓存）: $id")
      action(usersForId[id])
    } else {
      LOG.info("获取用户数据（DB)：$id")
      UserDao.getUserById(id) {
        it?.let {
          var user = it.mapTo(User::class.java)
          cacheUser(user)
          action(user)
        }?: action(null)
      }
    }
  }

  fun getUsersByIds(ids: List<Long>, action: (Map<Long, User>) -> Unit) {
    var itemMap = mutableMapOf<Long, User>()
    var idsForDB = mutableListOf<Long>()
    if (ids.isEmpty()) {
      action(itemMap)
      return
    }

    for (id in ids) {
      when {
        itemMap.containsKey(id) -> continue
        usersForId.containsKey(id) -> {
          usersForId[id]?.let {
            itemMap[id] = it
          }
        }
        else -> idsForDB.add(id)
      }
    }

    LOG.info("获取用户数据（缓存）：$itemMap")
    when {
      idsForDB.isEmpty() -> action(itemMap)
      else -> {
        var idStr = joiner.join(idsForDB)
        LOG.info("获取用户数据（DB）：$idStr")
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
    }
  }

  fun syncToDB(id: Long, action: (Boolean) -> Unit) {
    when {
      usersForId.containsKey(id) -> {
        var user = usersForId[id]
        UserDao.updateUserById(id, JsonObject.mapFrom(user)) {
          action(it)
        }
      }
      else -> action(false)
    }
  }

  // 私有方法
  private fun cacheUser(user: User) {
    usersForId[user.id] = user
    usersForName[user.username] = user
  }

}
