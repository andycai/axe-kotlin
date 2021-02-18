package com.iwayee.activity.define

enum class ErrCode(errorCode: Int, errorDesc: String) {
  SUCCESS(0, "成功"),

  ERR_REGISTER(-104, "注册失败！"),
  ERR_AUTH(-103, "登录验证失败，请重新登录！"),
  ERR_OP(-102, "操作失败！"),
  ERR_DATA(-101, "数据错误！"),
  ERR_PARAM(-100, "参数错误！"),

  ERR_ACTIVITY_OVER_QUOTA(-307, "报名候补的数量超出限制，请稍后再报名！"),
  ERR_ACTIVITY_NOT_ENOUGH(-306, "取消报名的数量不正确！"),
  ERR_ACTIVITY_FEE(-305, "选择活动前结算的活动，必须要填写费用！"),
  ERR_ACTIVITY_CREATE(-304, "创建新活动失败！"),
  ERR_ACTIVITY_NOT_PLANNER(-303, "你不是活动发起人，不能更新活动信息！"),
  ERR_ACTIVITY_UPDATE(-302, "更新活动信息失败！"),
  ERR_ACTIVITY_CANNOT_APPLY_NOT_IN_GROUP(-301, "你不是群组成员不能报名或取消报名群组活动！"),
  ERR_ACTIVITY_NO_DATA(-300, "找不活动数据！"),

  ERR_GROUP_MANAGER_LIMIT(-206, "副群主数量超过限制，不能再委任！"),
  ERR_GROUP_APPROVE(-205, "入群审批失败！"),
  ERR_GROUP_GET_DATA(-204, "获取群数据失败！"),
  ERR_GROUP_UPDATE_OP(-203, "更新群信息失败！"),
  ERR_GROUP_NOT_MANAGER(-202, "不是群管理员，没权限操作！"),
  ERR_GROUP_TRANSFER(-201, "不是群主，转让群主给其他成员！"),
  ERR_GROUP_PROMOTE(-200, "不是群主，不能委任副群主！"),
  ;

  var errorCode: Int = errorCode
  var errorDesc: String = errorDesc
}
