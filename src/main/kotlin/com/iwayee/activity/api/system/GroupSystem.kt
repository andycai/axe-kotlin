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
      it?.let {
        if (it.members.isEmpty) {
          return@let
        }
        var ids = mutableListOf<Int>()
        for (item in it.members) {
          ids.add((item as JsonObject).getInteger("id"))
        }

        UserCache.getUsersByIds(ids) { users ->
          if (users.isEmpty()) {
            some.err(ErrCode.ERR_DATA)
            return@getUsersByIds
          }

          var members = UserCache.toMember(users, it.members)
          var jo = JsonObject.mapFrom(it)
          jo.put("members", members)
          some.ok(jo)
        }
        return@getGroupById
      }
      some.err(ErrCode.ERR_DATA)
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
      if (it <= 0L) {
        some.err(ErrCode.ERR_OP)
      } else {
        some.ok(JsonObject().put("group_id", it))
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
      if (group == null) {
        some.err(ErrCode.ERR_GROUP_GET_DATA)
      } else if (!group.isManager(some.userId)) {
        some.err(ErrCode.ERR_GROUP_NOT_MANAGER)
      } else {
        group.name = name
        group.addr = addr
        group.logo = logo
        group.notice = notice
        GroupDao.updateGroupById(gid, JsonObject.mapFrom(group)) {
          if (it) {
            some.succeed()
          } else {
            some.err(ErrCode.ERR_GROUP_UPDATE_OP)
          }
        }
      }
    }
  }

  fun getApplyList(some: Some) {
    var gid = some.getUInt("gid")
    GroupCache.getGroupById(gid) { group ->
      if (group != null && !group.pending.isEmpty) {
        var ids = (group.pending.list as List<Int>)
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
      } else {
        some.err(ErrCode.ERR_GROUP_GET_DATA)
      }
    }
  }

  fun apply(some: Some) {
    var gid = some.getUInt("gid")
    var uid = some.userId

    GroupCache.getGroupById(gid) { group ->
      if (group != null && !group.pending.contains(uid)) {
        group.pending.add(uid)
        GroupCache.syncToDB(group.id) {
          if (it) {
            some.err(ErrCode.ERR_GROUP_UPDATE_OP)
          } else {
            some.succeed()
          }
        }
      } else {
        some.err(ErrCode.ERR_GROUP_GET_DATA)
      }
    }
  }

  fun approve(some: Some) {
    var gid = some.getUInt("gid")
    var pass = some.jsonBool("pass")
    var index = some.jsonInt("index")

    var uid = some.userId
    GroupCache.getGroupById(gid) { group ->
      if (group == null) {
        some.err(ErrCode.ERR_GROUP_GET_DATA)
      } else if (!group.isManager(uid) || index >= group.pending.size()) {
        some.err(ErrCode.ERR_GROUP_APPROVE)
      } else {
        var tid = group.pending.getInteger(index)
        if (group.notIn(tid)) {
          if (pass) {
            var jo = JsonObject()
            jo.put("id", tid)
                    .put("pos", GroupPosition.POS_MEMBER.ordinal)
                    .put("at", Date().time)
            group.members.add(jo)
          }
          group.pending.remove(tid)
          GroupCache.syncToDB(group.id) {
            if (it) {
              some.succeed()
            } else {
              some.err(ErrCode.ERR_GROUP_UPDATE_OP)
            }
          }
        } else {
          some.err(ErrCode.ERR_GROUP_APPROVE)
        }
      }
    }
  }

  fun promote(some: Some) {
    var gid = some.getUInt("gid")
    var mid = some.getUInt("mid")
    var uid = some.userId
    GroupCache.getGroupById(gid) { group ->
      if (group == null) {
        some.err(ErrCode.ERR_GROUP_GET_DATA)
      } else if (!group.isOwner(uid) || !group.promote(mid)) {
        some.err(ErrCode.ERR_GROUP_PROMOTE)
      } else {
        GroupDao.updateGroupById(gid, JsonObject.mapFrom(group)) {
          if (it) {
            some.succeed()
          } else {
            some.err(ErrCode.ERR_GROUP_UPDATE_OP)
          }
        }
      }
    }
  }

  fun transfer(some: Some) {
    var gid = some.getUInt("gid")
    var mid = some.getUInt("mid")
    var uid = some.userId
    GroupCache.getGroupById(gid) { group ->
      if (group == null) {
        some.err(ErrCode.ERR_GROUP_GET_DATA)
      } else if (!group.isOwner(uid) || !group.transfer(uid, mid)) {
        some.err(ErrCode.ERR_GROUP_TRANSFER)
      } else {
        GroupDao.updateGroupById(gid, JsonObject.mapFrom(group)) {
          if (it) {
            some.succeed()
          } else {
            some.err(ErrCode.ERR_GROUP_UPDATE_OP)
          }
        }
      }
    }
  }
}
