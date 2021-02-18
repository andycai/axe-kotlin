package com.iwayee.activity.api.system

import com.iwayee.activity.api.comp.Activity
import com.iwayee.activity.api.comp.Group
import com.iwayee.activity.cache.ActivityCache
import com.iwayee.activity.cache.GroupCache
import com.iwayee.activity.cache.UserCache
import com.iwayee.activity.dao.mysql.ActivityDao
import com.iwayee.activity.define.ActivityFeeType
import com.iwayee.activity.define.ActivityStatus
import com.iwayee.activity.define.ErrCode
import com.iwayee.activity.hub.Some
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

object ActivitySystem {
  private fun doCreate(some: Some, jo: JsonObject, uid: Long, group: Group?) {
    ActivityCache.create(jo, uid) { newId ->
      when {
        newId <= 0L -> some.err(ErrCode.ERR_ACTIVITY_CREATE)
        else -> {
          // 更新用户的活动列表
          UserCache.getUserById(uid) { user ->
            when (user) {
              null -> some.err(ErrCode.ERR_ACTIVITY_CREATE)
              else -> {
                user.addActivity(newId)
                UserCache.syncToDB(uid) { b ->
                  when (b) {
                    true -> {
                      // 更新群组的活动列表
                      when (group) {
                        null -> some.ok(JsonObject().put("activity_id", newId))
                        else -> {
                          group.addActivity(newId.toInt())
                          GroupCache.syncToDB(group.id) { b ->
                            when (b) {
                              true -> some.ok(JsonObject().put("activity_id", newId))
                              else -> some.err(ErrCode.ERR_ACTIVITY_CREATE)
                            }
                          }
                        }
                      }
                    }
                    else -> some.err(ErrCode.ERR_ACTIVITY_CREATE)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  fun create(some: Some) {
    var uid = some.userId // 通过 session 获取
    var gid = some.jsonInt("group_id")
    var feeType = some.jsonUInt("fee_type")
    var jo = JsonObject()
    jo.put("planner", uid)
            .put("group_id", gid)
            .put("kind", some.jsonUInt("kind"))
            .put("type", some.jsonUInt("type"))
            .put("quota", some.jsonUInt("quota"))
            .put("fee_type", some.jsonUInt("fee_type"))
            .put("fee_male", some.jsonInt("fee_male"))
            .put("fee_female", some.jsonInt("fee_female"))
            .put("ahead", some.jsonUInt("ahead"))
            .put("title", some.jsonStr("title"))
            .put("remark", some.jsonStr("remark"))
            .put("addr", some.jsonStr("addr"))
            .put("begin_at", some.jsonStr("begin_at"))
            .put("end_at", some.jsonStr("end_at"))
            .put("queue", "[$uid]")
            .put("queue_sex", "[${some.userSex}]")
            .put("status", ActivityStatus.DOING.ordinal)
    // 活动前结算，必须填写费用
    var feeErr = false
    when (feeType) {
      ActivityFeeType.FEE_TYPE_BEFORE.ordinal -> feeErr = (some.jsonInt("fee_male") == 0 || some.jsonInt("fee_female") == 0)
      ActivityFeeType.FEE_TYPE_AFTER_AA.ordinal -> feeErr = (some.jsonInt("fee_male") != 0 || some.jsonInt("fee_female") != 0)
      ActivityFeeType.FEE_TYPE_AFTER_AB.ordinal -> feeErr = (some.jsonInt("fee_male") == 0 || some.jsonInt("fee_female") != 0)
      ActivityFeeType.FEE_TYPE_AFTER_BA.ordinal -> feeErr = (some.jsonInt("fee_male") != 0 || some.jsonInt("fee_female") == 0)
    }

    when {
      feeErr -> some.err(ErrCode.ERR_ACTIVITY_FEE)
      gid > 0 -> {
        GroupCache.getGroupById(gid) { group ->
          when {
            group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
            group.isManager(uid) -> doCreate(some, jo, uid, group)
            else -> some.err(ErrCode.ERR_GROUP_NOT_MANAGER)
          }
        }
      }
      else -> doCreate(some, jo, uid, null)
    }
  }

  fun getActivitiesByUserId(some: Some) {
    var uid = some.userId
    UserCache.getUserById(uid) { user ->
      when {
        user == null -> some.err(ErrCode.ERR_DATA)
        user.activities.isEmpty -> some.ok(JsonArray())
        else -> {
          var ids = user.activities.list as List<Long>
          ActivityCache.getActivitiesByIds(ids) { acts ->
            var jr = JsonArray()
            for (item in acts) {
              jr.add((item as Activity).toJson())
            }
            some.ok(jr)
          }
        }
      }
    }
  }

  fun getActivitiesByGroupId(some: Some) {
    var gid = some.getUInt("gid")

    GroupCache.getGroupById(gid) { group ->
      when {
        group == null -> some.err(ErrCode.ERR_DATA)
        group.activities.isEmpty -> some.ok(JsonArray())
        else -> {
          var ids = group.activities.list as List<Long>
          ActivityCache.getActivitiesByIds(ids) { acts ->
            var jr = JsonArray()
            for (item in acts) {
              jr.add((item as Activity).toJson())
            }
            some.ok(jr)
          }
        }
      }
    }
  }

  fun getActivities(some: Some) {
    var type = some.jsonUInt("type")
    var status = some.jsonUInt("status")
    var page = some.jsonUInt("page")
    var num = some.jsonUInt("num")

    ActivityCache.getActivitiesByType(type, status, page, num) { acts ->
      var jr = JsonArray()
      for (item in acts) {
        jr.add((item as Activity).toJson())
      }
      some.ok(jr)
    }
  }

  fun getActivityById(some: Some) {
    var aid = some.getULong("aid")
    ActivityCache.getActivityById(aid) { activity ->
      when (activity) {
        null -> some.err(ErrCode.ERR_DATA)
        else -> {
          var ids = activity.queue.list as List<Long>
          UserCache.getUsersByIds(ids) { users ->
            when (users) {
              null -> some.err(ErrCode.ERR_DATA)
              else -> {
                var players = UserCache.toPlayer(users)
                var ret = JsonObject.mapFrom(activity)
                ret.put("players", players)
                some.ok(ret)
              }
            }
          }
        }
      }
    }
  }

  private fun doUpdate(some: Some, act: Activity) {
    ActivityCache.syncToDB(act.id) { b ->
      when (b) {
        true -> some.succeed()
        else -> some.err(ErrCode.ERR_ACTIVITY_UPDATE)
      }
    }
  }

  fun update(some: Some) {
    var aid = some.getULong("aid")
    var quota = some.jsonUInt("quota")
    var ahead = some.jsonUInt("ahead")
    var feeMale = some.jsonInt("fee_male")
    var feeFemale = some.jsonInt("fee_female")
    var title = some.jsonStr("title")
    var remark = some.jsonStr("remark")
    var addr = some.jsonStr("addr")
    var beginAt = some.jsonStr("begin_at")
    var endAt = some.jsonStr("end_at")
    var uid = some.userId

    ActivityCache.getActivityById(aid) { act ->
      act?.let { activity ->
        activity.quota = quota
        activity.ahead = ahead
        activity.fee_male = feeMale
        activity.fee_female = feeFemale
        activity.title = title
        activity.remark = remark
        activity.addr = addr
        activity.begin_at = beginAt
        activity.end_at = endAt
        when {
          activity.inGroup() -> {
            GroupCache.getGroupById(activity.group_id) { group ->
              when {
                group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
                group.isManager(uid) -> doUpdate(some, activity)
                else -> some.err(ErrCode.ERR_GROUP_NOT_MANAGER)
              }
            }
          }
          uid != activity.planner -> some.err(ErrCode.ERR_ACTIVITY_NOT_PLANNER)
          else -> doUpdate(some, activity)
        }
      } ?: let {
        some.err(ErrCode.ERR_ACTIVITY_NO_DATA)
      }
    }
  }

  private fun doEnd(some: Some, fee: Int, aid: Long, act: Activity) {
    // 结算或者终止
    act.settle(fee)
    var jo = JsonObject()
    jo.put("status", act.status)
            .put("fee_male", act.fee_male)
            .put("fee_female", act.fee_female)

    ActivityDao.updateActivityStatus(aid, jo) { b ->
      when (b) {
        true -> some.succeed()
        else -> some.err(ErrCode.ERR_OP)
      }
    }
  }

  fun end(some: Some) {
    var aid = some.getULong("aid")
    var fee = some.jsonInt("fee") // 单位：分
    var uid = some.userId

    ActivityCache.getActivityById(aid) { activity ->
      when {
        activity == null -> some.err(ErrCode.ERR_ACTIVITY_NO_DATA)
        activity.inGroup() -> {
          GroupCache.getGroupById(activity.group_id) { group ->
            when {
              group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
              group.isManager(uid) -> doEnd(some, fee, aid, activity)
              else -> some.err(ErrCode.ERR_GROUP_NOT_MANAGER)
            }
          }
        }
        else -> doEnd(some, fee, aid, activity)
      }
    }
  }

  private fun enqueue(some: Some, uid: Long, act: Activity, maleCount: Int, femaleCount: Int) {
    act.enqueue(uid, maleCount, femaleCount)
    ActivityCache.syncToDB(act.id) { b ->
      when (b) {
        true -> some.succeed()
        else -> some.err(ErrCode.ERR_ACTIVITY_UPDATE)
      }
    }
  }

  /**
   * 报名，支持带多人报名
   */
  fun apply(some: Some) {
    var aid = some.getULong("aid")
    var uid = some.userId
    var maleCount = some.jsonInt("male_count")
    var femaleCount = some.jsonInt("female_count")

    ActivityCache.getActivityById(aid) { activity ->
      when {
        activity == null -> some.err(ErrCode.ERR_ACTIVITY_NO_DATA)
        activity.overQuota((maleCount + femaleCount)) -> some.err(ErrCode.ERR_ACTIVITY_OVER_QUOTA)
        activity.inGroup() -> {
          GroupCache.getGroupById(activity.group_id) { group ->
            when {
              group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
              group.isMember(uid) -> enqueue(some, uid, activity, maleCount, femaleCount)
              else -> some.err(ErrCode.ERR_ACTIVITY_CANNOT_APPLY_NOT_IN_GROUP)
            }
          }
        }
        else -> enqueue(some, uid, activity, maleCount, femaleCount)
      }
    }
  }

  private fun dequeue(some: Some, uid: Long, act: Activity, maleCount: Int, femaleCount: Int) {
    act.dequeue(uid, maleCount, femaleCount)
    ActivityCache.syncToDB(act.id) { b ->
      when (b) {
        true -> some.succeed()
        else -> some.err(ErrCode.ERR_ACTIVITY_UPDATE)
      }
    }
  }

  /**
   * 取消报名，支持取消自带的多人
   */
  fun cancel(some: Some) {
    var aid = some.getULong("aid");
    var uid = some.userId;
    var maleCount = some.jsonInt("male_count");
    var femaleCount = some.jsonInt("female_count");

    if (maleCount + femaleCount <= 0) {
      some.err(ErrCode.ERR_PARAM)
      return
    }

    ActivityCache.getActivityById(aid) { activity ->
      when {
        activity == null -> some.err(ErrCode.ERR_ACTIVITY_NO_DATA)
        activity.notEnough(uid, (maleCount + femaleCount)) -> some.err(ErrCode.ERR_ACTIVITY_NOT_ENOUGH)
        activity.inGroup() -> {
          GroupCache.getGroupById(activity.group_id) { group ->
            when {
              group == null -> some.err(ErrCode.ERR_GROUP_GET_DATA)
              group.isMember(uid) -> dequeue(some, uid, activity, maleCount, femaleCount)
              else -> some.err(ErrCode.ERR_ACTIVITY_CANNOT_APPLY_NOT_IN_GROUP)
            }
          }
        }
        else -> dequeue(some, uid, activity, maleCount, femaleCount)
      }
    }
  }
}
