package com.iwayee.activity.api.system

import com.iwayee.activity.api.comp.User
import com.iwayee.activity.cache.UserCache
import com.iwayee.activity.define.ErrCode
import com.iwayee.activity.hub.Some
import com.iwayee.activity.utils.Encrypt
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

object UserSystem {
  private fun user2Json(user: User): JsonObject {
    var jo = JsonObject.mapFrom(user)
    jo.remove("password")
    return jo
  }

  fun login(some: Some) {
    val name = some.jsonStr("username")
    val wxNick = some.jsonStr("wx_nick")
    val sex = some.jsonUInt("sex")

    UserCache.getUserByName(name) { it ->
      if (it == null) {
        var ip = some.getIP()
        var jo = JsonObject()
        jo.put("username", name)
                .put("password", Encrypt.md5("123456"))
                .put("token", Encrypt.md5(name))
                .put("wx_token", Encrypt.md5(name))
                .put("wx_nick", wxNick)
                .put("nick", "")
                .put("sex", sex)
                .put("phone", "")
                .put("email", "")
                .put("ip", ip)
                .put("activities", "[]")
                .put("groups", "[]")
        UserCache.create(jo) { newId ->
          when (newId) {
            0L -> some.err(ErrCode.ERR_OP)
            else -> {
              var token = jo.getString("token")
              UserCache.cacheSession(token, newId, sex)
              UserCache.getUserById(newId) { user ->
                user?.let {
                  some.ok(user2Json(user))
                }?:let {
                  some.err(ErrCode.ERR_AUTH)
                }
              }
            }
          }
        }
      } else {
        UserCache.cacheSession(it.token, it.id, it.sex)
        some.ok(user2Json(it))
      }
    }
  }

  fun wxLogin(some: Some) {
    //
  }

  fun register(some: Some) {
    //
  }

  fun logout(some: Some) {
    UserCache.clearSession(some.token)
    some.succeed()
  }

  fun getUserByName(some: Some) {
    val name = some.getStr("username");

    UserCache.getUserByName(name) {
      it?.let {
        some.ok(user2Json(it))
      }?:let {
        some.err(ErrCode.ERR_USER_DATA)
      }
    }
  }

  fun getUser(some: Some) {
    val uid = some.getULong("uid");

    UserCache.getUserById(uid) {
      it?.let {
        some.ok(user2Json(it))
      }?:let {
        some.err(ErrCode.ERR_USER_DATA)
      }
    }
  }

  // 内部接口，非路由handler

  fun enterGroup(some: Some, gid: Int, uid: Long) {
    UserCache.getUserById(uid) {
      it?.let {
        it.addGroup(gid)
        saveData(some, uid)
      }?:let {
        some.err(ErrCode.ERR_USER_DATA)
      }
    }
  }

  fun quitGroup(some: Some, gid: Int, uid: Long) {
    UserCache.getUserById(uid) {
      it?.let {
        it.removeGroup(gid)
        saveData(some, uid)
      }?:let {
        some.err(ErrCode.ERR_USER_DATA)
      }
    }
  }

  fun applyActivity(some: Some, aid: Long, uid: Long) {
    UserCache.getUserById(uid) {
      it?.let {
        if (it.containsActivity(aid)) {
          some.succeed()
        } else {
          it.addActivity(aid)
          saveData(some, uid)
        }
      }?:let {
        some.err(ErrCode.ERR_USER_DATA)
      }
    }
  }

  fun cancelActivity(some: Some, aid: Long, uid: Long) {
    UserCache.getUserById(uid) {
      it?.let {
        it.removeActivity(aid)
        saveData(some, uid)
      }?: some.err(ErrCode.ERR_USER_DATA)
    }
  }

  // 私有方法
  private fun saveData(some: Some, uid: Long) {
    UserCache.syncToDB(uid) { ok ->
      when (ok) {
        true -> some.succeed()
        else -> some.err(ErrCode.ERR_USER_UPDATE_DATA)
      }
    }
  }
}
