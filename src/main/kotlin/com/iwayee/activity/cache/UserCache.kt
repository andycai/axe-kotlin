package com.iwayee.activity.cache

import com.iwayee.activity.api.comp.Session
import com.iwayee.activity.api.comp.User
import com.iwayee.activity.dao.mysql.UserDao
import java.util.*

object UserCache: BaseCache() {
  private var userForName = mutableMapOf<String, User>()
  private var userForId = mutableMapOf<Int, User>()
  private var sessions = mutableMapOf<String, Session>()

  fun cacheSession(token: String, uid: Int, sex: Int) {
    var session : Session? = null
    if (sessions.containsKey(token)) {
      session = sessions[token]
    } else {
      session = Session(uid)
      sessions[token] = session
    }
    session?.let{
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
      return (now - s.at) > (2*24*60*60*1000)
    }
    return true
  }

  private fun cacheUser(user: User) {
    userForId[user.id] = user
    userForName[user.username] = user
  }

  fun getUserById(id: Int, action: (User?) -> Unit) {
    if (userForId.containsKey(id)) {
      println("获取用户数据（缓存）: $id")
      action(userForId[id])
    } else {
      println("获取用户数据（DB)：$id")
      UserDao.getUserByID(id) {
        it?.let {
          var user = it.mapTo(User::class.java)
          cacheUser(user)
          action(user)
          return@getUserByID
        }
        action(null)
      }
    }
  }
}
