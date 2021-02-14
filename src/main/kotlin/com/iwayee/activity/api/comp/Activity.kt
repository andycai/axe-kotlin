package com.iwayee.activity.api.comp

import com.iwayee.activity.define.ActivityFeeType
import com.iwayee.activity.define.ActivityKind
import com.iwayee.activity.define.ActivityStatus
import com.iwayee.activity.define.ActivityType
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class Activity(
        var id: Int = 0,
        var planner: Int = 0,
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
        var queue: JsonArray = JsonArray(), // 报名队列
        var queue_sex: JsonArray = JsonArray() // 报名队列中的性别
) {
  fun toJson(): JsonObject {
    var jo = JsonObject()
    jo.put("id", id)
            .put("status", status)
            .put("quota", quota)
            .put("count", queue.size())
            .put("title", title)
            .put("remark", remark)
            .put("begin_at", begin_at)
            .put("end_at", end_at)
    return jo
  }
}
