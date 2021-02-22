package com.iwayee.activity.api.comp

import com.iwayee.activity.define.*
import com.iwayee.activity.utils.DateUtils.getDiffHours
import io.vertx.core.json.JsonObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.round

data class Activity(
        var id: Long = 0,
        var planner: Long = 0,
        var kind: Int = ActivityKind.KIND_DATE.ordinal, // 活动分类:1羽毛球,2篮球,3足球,4聚餐...
        var type: Int = ActivityType.PUBLIC.ordinal, // 活动类型:1全局保护,2全局公开,3群组
        var status: Int = ActivityStatus.DOING.ordinal, // 活动状态:1进行中,2正常结算完成,3手动终止
        var quota: Int = 2, // 名额
        var group_id: Int = 0, // 群组ID
        var ahead: Int = 6, // 提前取消报名限制（单位：小时）
        var fee_type: Int = ActivityFeeType.FEE_TYPE_AFTER_AA.ordinal, // 结算方式:1免费,2活动前,3活动后男女平均,4活动后男固定|女平摊,5活动后男平摊|女固定
        var fee_male: Int = 0, // 男费用
        var fee_female: Int = 0, // 女费用
        var title: String = "",
        var remark: String = "",
        var addr: String = "",
        var begin_at: String = "",
        var end_at: String = "",
        var queue: MutableList<Long> = mutableListOf(), // 报名队列
        var queue_sex: MutableList<Int> = mutableListOf() // 报名队列中的性别
) {
  companion object {
    const val OVERFLOW = 10
  }

  fun inGroup(): Boolean {
    return group_id > 0
  }

  fun isPlanner(uid: Long): Boolean {
    return uid == planner
  }

  fun toJson(): JsonObject {
    var jo = JsonObject()
    jo.put("id", id)
            .put("status", status)
            .put("quota", quota)
            .put("count", queue.size)
            .put("title", title)
            .put("remark", remark)
            .put("begin_at", begin_at)
            .put("end_at", end_at)
    return jo
  }

  fun settle(fee: Int) {
    status = if (fee > 0) ActivityStatus.DONE.ordinal else ActivityStatus.END.ordinal
    when (fee_type) {
      ActivityFeeType.FEE_TYPE_AFTER_AA.ordinal -> {
        var cost = round(fee.toFloat() / totalCount()).toInt()
        fee_male = cost
        fee_female = cost
      }
      ActivityFeeType.FEE_TYPE_AFTER_AB.ordinal -> {
        fee_female = round((fee.toFloat() - (fee_male * maleCount())) / femaleCount()).toInt()
      }
      ActivityFeeType.FEE_TYPE_AFTER_BA.ordinal -> {
        fee_male = round((fee.toFloat() - (fee_female * femaleCount())) / maleCount()).toInt()
      }
    }
  }

  // 报名的人数超过候补的限制，避免乱报名，如带100000人报名
  fun overQuota(total: Int): Boolean {
    return queue.size + total - quota > OVERFLOW
  }

  // 要取消报名的数量超过已经报名的数量
  fun notEnough(uid: Long, total: Int): Boolean {
    var count = 0
    for (item in queue) {
      if (item == uid) {
        count += 1
      }
    }
    return total > count
  }

  fun inQueue(uid: Long): Boolean {
    return queue.contains(uid)
  }

  fun enqueue(uid: Long, maleCount: Int, femaleCount: Int) {
    fixQueue()
    for (i in 0 until maleCount) {
      queue.add(uid)
      queue_sex.add(SexType.MALE.ordinal) // 加入性别队列
    }
    for (i in 0 until femaleCount) {
      queue.add(uid)
      queue_sex.add(SexType.FEMALE.ordinal)
    }
  }

  fun getIdFromQueue(index: Int): Long {
    if (index < 0 || index >= queue.size) {
      return 0
    }
    return queue[index]
  }

  fun dequeue(index: Int): Boolean {
    fixQueue()
    if (index < 0 || index >= queue.size) {
      return false
    }
    queue.removeAt(index)
    queue_sex.removeAt(index)
    return true
  }

  fun dequeue(uid: Long, maleCount: Int, femaleCount: Int) {
    fixQueue()
    var mCount = 0
    var fCount = 0
    var size = queue.size
    var posArr = mutableListOf<Int>()
    for (i in size-1 downTo 0) {
      var id = queue[i]
      if (id == uid) {
        // 男
        if (queue_sex[i] == SexType.MALE.ordinal && maleCount > mCount) {
          mCount += 1
          posArr.add(i)
        }
        // 女
        if (queue_sex[i] == SexType.FEMALE.ordinal && femaleCount > fCount) {
          fCount += 1
          posArr.add(i)
        }
        if (mCount >= maleCount && fCount >= femaleCount) {
          break
        }
      }
    }
    var total = posArr.size
    for (i in 0 until total) {
      queue.removeAt(posArr[i])
      queue_sex.removeAt(posArr[i])
    }
  }

  fun hasBegun(): Boolean {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    val now = Date()
    var h: Long = 0
    try {
      val begin = fmt.parse(begin_at)
      h = getDiffHours(now, begin)
    } catch (e: ParseException) {
      e.printStackTrace()
    }
    return h <= 0
  }

  fun canCancel(): Boolean {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    val now = Date()
    var h: Long = 0
    try {
      val begin = fmt.parse(begin_at)
      h = getDiffHours(now, begin)
    } catch (e: ParseException) {
      e.printStackTrace()
    }
    return h >= ahead
  }

  // 私有方法
  private fun totalCount(): Int { // 最终确定报名人数
    var c: Int
    var size = queue.size
    c = when {
      quota >= size -> size
      else -> quota
    }
    return c
  }

  private fun maleCount(): Int {
    var c = 0
    var count = totalCount()
    for (i in 0 until count) {
      if (queue_sex[i] == SexType.MALE.ordinal) {
        c += 1
      }
    }
    return c
  }

  private fun femaleCount(): Int {
    var c = 0
    var count = totalCount()
    for (i in 0 until count) {
      if (queue_sex[i] == SexType.FEMALE.ordinal) {
        c += 1
      }
    }
    return c
  }

  private fun fixQueue() {
    var sizeSex = queue_sex.size
    var size = queue.size
    var df = sizeSex - size
    if (df > 0) {
      for (i in sizeSex-1 downTo sizeSex-df) {
        queue_sex.removeAt(i)
      }
    }
    if (df < 0) {
      for (i in size-1 downTo size-(abs(df))) {
        queue.removeAt(i)
      }
    }
  }
}
