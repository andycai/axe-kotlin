package com.iwayee.activity.api.system

import com.iwayee.activity.cache.GroupCache
import com.iwayee.activity.cache.UserCache
import com.iwayee.activity.dao.mysql.GroupDao
import com.iwayee.activity.define.ErrCode
import com.iwayee.activity.define.GroupPosition
import com.iwayee.activity.hub.Some
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
    var page = some.jsonUInt("page")
    var num = some.jsonUInt("num")

    GroupCache.getGroups(page, num) {
      some.ok(it)
    }
  }

  fun getGroupsByUserId(some: Some) {
    UserCache.getUserById(some.userId) { user ->
      user?.let {
        var ids: List<Int> = it.groups.list as List<Int>
        GroupCache.getGroupsByIds(ids) { data ->
          some.ok(data)
        }
      }
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
        else -> some.err(ErrCode.ERR_GROUP_NOT_MANAGER)
      }
    }
  }

  fun getApplyList(some: Some) {
    var gid = some.getUInt("gid")
    GroupCache.getGroupById(gid) { group ->
      when {
        group == null || group.pending.isEmpty -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        else -> {
          var ids = group.pending.list as List<Long>
          UserCache.getUsersByIds(ids) { users ->
            var jr = JsonArray()
            for ((key, value) in users) {
              var jo = JsonObject()
              var index = group.pending.list.indexOf(value.id)
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
        group.isManager(uid) -> {
          var tid = group.pending.getLong(index)
          if (pass && group.notIn(tid)) {
            var jo = JsonObject()
            jo.put("id", tid)
                    .put("scores", 0)
                    .put("pos", GroupPosition.POS_MEMBER.ordinal)
                    .put("at", Date().time)
            group.members.add(jo)
          }
          group.pending.remove(index)
          GroupCache.syncToDB(group.id) {
            when (it) {
              true -> some.succeed()
              else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
            }
          }
        }
        else -> some.err(ErrCode.ERR_GROUP_NOT_MANAGER)
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
        group.isOwner(uid) && group.promote(mid) -> {
          GroupCache.syncToDB(group.id) {
            when (it) {
              true -> some.succeed()
              else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
            }
          }
        }
        group.managerCount() >= 3 -> some.err(ErrCode.ERR_GROUP_MANAGER_LIMIT)
        else -> some.err(ErrCode.ERR_GROUP_PROMOTE)
      }
    }
  }

  fun transfer(some: Some) {
    var gid = some.getUInt("gid")
    var mid = some.getUInt("mid")
    var uid = some.userId
    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
        group.isOwner(uid) && group.transfer(uid, mid) -> {
          GroupCache.syncToDB(group.id) {
            when (it) {
              true -> some.succeed()
              else -> some.err(ErrCode.ERR_GROUP_UPDATE_OP)
            }
          }
        }
        else -> some.err(ErrCode.ERR_GROUP_TRANSFER)
      }
    }
  }
}
