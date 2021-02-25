package com.iwayee.activity.cache

import com.iwayee.activity.api.comp.Activity
import com.iwayee.activity.dao.mysql.ActivityDao
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

object ActivityCache : BaseCache() {
  private var activities = mutableMapOf<Long, Activity>()
  private val LOG = LoggerFactory.getLogger(ActivityCache::class.java)

  fun create(jo: JsonObject, uid: Long, action: (Long) -> Unit) {
    ActivityDao.create(jo) { newId ->
      when {
        newId <= 0L -> action(newId)
        else -> {
          var activity = jo.mapTo(Activity::class.java)
          activity.id = newId
          cache(activity)
          action(newId)
        }
      }
    }
  }

  fun getActivityById(id: Long, action: (Activity?) -> Unit) {
    if (activities.containsKey(id)) {
      LOG.info("获取活动数据（缓存）: $id")
      action(activities[id])
    } else {
      LOG.info("获取活动数据（DB)：$id")
      ActivityDao.getActivityById(id) {
        it?.let {
          var activity = it.mapTo(Activity::class.java)
          cache(activity)
          action(activity)
        }
      }
    }
  }

  fun getActivitiesByIds(ids: List<Long>, action: (JsonArray) -> Unit) {
    var jr = JsonArray()
    var idsForDB = mutableListOf<Long>()
    if (ids.isEmpty()) {
      action(jr)
      return
    }

    for (id in ids) {
      when {
        jr.contains(id) -> continue
        activities.containsKey(id) -> {
          activities[id]?.let {
            jr.add(it)
          }
        }
        else -> idsForDB.add(id)
      }
    }

    LOG.info("获取活动数据（缓存）：${jr.encode()}")
    when {
      idsForDB.isEmpty() -> action(jr)
      else -> {
        var idStr = joiner.join(idsForDB)
        LOG.info("获取活动数据（DB）：$idStr")
        ActivityDao.getActivitiesByIds(idStr) {
          it?.forEach { entry ->
            var jo = entry as JsonObject
            var activity = jo.mapTo(Activity::class.java)
            cache(activity)
            jr.add(activity)
          }
          action(jr)
        }
      }
    }
  }

  fun getActivitiesByType(type: Int, status: Int, page: Int, num: Int, action: (JsonArray) -> Unit) {
    // 缓存60秒
    ActivityDao.getActivitiesByType(type, status, page, num) {
      var jr = JsonArray()
      when {
        it.isEmpty -> action(jr)
        else -> {
          it.forEach { v ->
            var activity = (v as JsonObject).mapTo(Activity::class.java)
            cache(activity)
            jr.add(activity)
          }
          action(jr)
        }
      }
    }
  }

  fun syncToDB(id: Long, action: (Boolean) -> Unit) {
    when {
      activities.containsKey(id) -> {
        var activity = activities[id]
        ActivityDao.updateActivityById(id, JsonObject.mapFrom(activity)) {
          action(it)
        }
      }
      else -> action(false)
    }
  }

  // 私有方法
  private fun cache(activity: Activity) {
    activity?.let {
      activities[it.id] = it
    }
  }
}
