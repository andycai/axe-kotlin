package com.iwayee.activity.cache

import com.iwayee.activity.api.comp.Session
import com.iwayee.activity.api.comp.User
import com.iwayee.activity.dao.mysql.UserDao

object UserCache: BaseCache() {
  private var userForName = mutableMapOf<String, User>()
  private var userForId = mutableMapOf<Int, User>()
  private var sessions = mutableMapOf<Int, Session>()

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
