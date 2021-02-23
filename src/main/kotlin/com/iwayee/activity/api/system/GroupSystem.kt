package com.iwayee.activity.api.system

import com.iwayee.activity.cache.GroupCache
import com.iwayee.activity.cache.UserCache
import com.iwayee.activity.dao.mysql.GroupDao
import com.iwayee.activity.define.ErrCode
import com.iwayee.activity.define.GroupPosition
import com.iwayee.activity.hub.Some
import com.iwayee.activity.utils.CollectionUtils
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.*

object GroupSystem {
  fun getGroupById(some: Some) {
    var gid = some.getUInt("gid")

    GroupCache.getGroupById(gid) {
      when {
        it == null || it.members.isEmpty -> some.err(ErrCode.ERR_DATA)
        else -> {
          var ids = mutableListOf<Long>()
          for (item in it.members) {
            ids.add((item as JsonObject).getLong("id"))
          }

          UserCache.getUsersByIds(ids) { users ->
            when {
              users.isEmpty() -> some.err(ErrCode.ERR_DATA)
              else -> {
                var members = UserCache.toMember(users, it.members)
                var jo = JsonObject.mapFrom(it)
                jo.put("members", members)
                some.ok(jo)
              }
            }
          }
        }
      }
    }
  }

  fun getGroups(some: Some) {
    val page = some.jsonUInt("page")
    val num = some.jsonUInt("num")

    GroupCache.getGroups(page, num) {
      some.ok(it)
    }
  }

  fun getGroupsByUserId(some: Some) {
    val page = some.jsonUInt("page")
    val num = some.jsonUInt("num")

    UserCache.getUserById(some.userId) { user ->
      user?.let {
        var jr = JsonArray()
        var ids = CollectionUtils.subLastList(it.groups, page, num)
        when {
          ids.isEmpty() -> some.ok(jr)
          else -> {
            GroupCache.getGroupsByIds(it.groups) { data ->
              some.ok(data)
            }
          }
        }
      }?:some.err(ErrCode.ERR_USER_DATA)
    }
  }

  fun create(some: Some) {
    var jo = JsonObject()
    jo.put("name", some.jsonStr("name"))
            .put("logo", some.jsonStr("logo"))
            .put("addr", some.jsonStr("addr"))

    GroupCache.create(jo, some.userId) {
      when {
        it <= 0L -> some.err(ErrCode.ERR_OP)
        else -> some.ok(JsonObject().put("group_id", it))
      }
    }
  }

  fun updateGroup(some: Some) {
    var gid = some.getUInt("gid")
    var name = some.jsonStr("name")
    var addr = some.jsonStr("addr")
    var logo = some.jsonStr("logo")
    var notice = some.jsonStr("notice")

    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        group.isManager(some.userId) -> {
          group.name = name
          group.addr = addr
          group.logo = logo
          group.notice = notice
          GroupDao.updateGroupById(gid, JsonObject.mapFrom(group)) {
            when (it) {
              true -> some.succeed()
              else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
            }
          }
        }
        else -> some.err(ErrCode.ERR_GROUP_NON_MANAGER)
      }
    }
  }

  fun getApplyList(some: Some) {
    var gid = some.getUInt("gid")
    GroupCache.getGroupById(gid) { group ->
      when {
        group == null || group.pending.isEmpty() -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        else -> {
          UserCache.getUsersByIds(group.pending) { users ->
            var jr = JsonArray()
            for ((key, value) in users) {
              var jo = JsonObject()
              var index = group.pending.indexOf(value.id)
              jo.put("id", value.id)
                      .put("nick", value.nick)
                      .put("wx_nick", value.wx_nick)
                      .put("index", index)
              jr.add(jo)
            }
            some.ok(jr)
          }
        }
      }
    }
  }

  fun apply(some: Some) {
    var gid = some.getUInt("gid")
    var uid = some.userId

    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        group.pending.contains(uid) -> some.succeed()
        else -> {
          group.pending.add(uid)
          GroupCache.syncToDB(group.id) {
            when (it) {
              true -> some.succeed()
              else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
            }
          }
        }
      }
    }
  }

  fun approve(some: Some) {
    var gid = some.getUInt("gid")
    var pass = some.jsonBool("pass")
    var index = some.jsonInt("index")
    var uid = some.userId

    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        group.notInPending(index) -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        !group.isManager(uid) -> some.err(ErrCode.ERR_GROUP_NON_MANAGER)
        else -> {
          var tid = group.pending[index]
          when {
            !group.notIn(tid) -> some.err(ErrCode.ERR_GROUP_APPROVE)
            else -> {
              if (pass) {
                var jo = JsonObject()
                jo.put("id", tid)
                        .put("scores", 0)
                        .put("pos", GroupPosition.POS_MEMBER.ordinal)
                        .put("at", Date().time)
                group.members.add(jo)
              }
              group.pending.removeAt(index)
              GroupCache.syncToDB(group.id) {
                when (it) {
                  true -> {
                    if (pass) {
                      // 加入群组，更新用户的群组列表
                      UserSystem.enterGroup(some, gid, tid)
                    }
                  }
                  else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
                }
              }
            }
          }
        }
      }
    }
  }

  fun promote(some: Some) {
    var gid = some.getUInt("gid")
    var mid = some.getULong("mid")
    var uid = some.userId
    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        !group.isOwner(uid) -> some.err(ErrCode.ERR_GROUP_NON_OWNER)
        !group.promote(mid) -> some.err(ErrCode.ERR_GROUP_PROMOTE)
        group.managerCount() >= 3 -> some.err(ErrCode.ERR_GROUP_MANAGER_LIMIT)
        else -> saveData(some, gid)
      }
    }
  }

  fun transfer(some: Some) {
    val gid = some.getUInt("gid")
    val mid = some.getULong("mid")
    val uid = some.userId
    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        !group.isOwner(uid) -> some.err(ErrCode.ERR_GROUP_NON_OWNER)
        !group.transfer(uid, mid) -> some.err(ErrCode.ERR_GROUP_TRANSFER)
        else -> saveData(some, gid)
      }
    }
  }

  fun remove(some: Some) {
    val gid = some.getUInt("gid")
    val mid = some.getULong("mid")
    val uid = some.userId
    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        !group.isManager(uid) -> some.err(ErrCode.ERR_GROUP_NON_MANAGER)
        !group.remove(mid) -> some.err(ErrCode.ERR_GROUP_REMOVE)
        // 同时删除用户的群组列表数据
        else -> {
          GroupCache.syncToDB(gid) { ok ->
            when (ok) {
              true -> UserSystem.quitGroup(some, gid, mid)
              else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
            }
          }
        }
      }
    }
  }

  fun quit(some: Some) {
    val gid = some.getUInt("gid")
    val uid = some.userId
    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        !group.isMember(uid) -> some.err(ErrCode.ERR_GROUP_NON_MEMBER)
        !group.remove(uid) -> some.err(ErrCode.ERR_GROUP_REMOVE)
        // 同时删除用户的群组列表数据
        else -> {
          GroupCache.syncToDB(gid) { ok ->
            when (ok) {
              true -> UserSystem.quitGroup(some, gid, uid)
              else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
            }
          }
        }
      }
    }
  }

  // 私有方法
  private fun saveData(some: Some, gid: Int) {
    GroupCache.syncToDB(gid) { ok ->
      when (ok) {
        true -> some.succeed()
        else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
      }
    }
  }
}
