package com.iwayee.activity.api.comp

import com.iwayee.activity.define.GroupPosition
import com.iwayee.activity.define.SexType

data class Member(
        var id: Long = 0,
        var scores: Int = 0,
        var pos: Int = GroupPosition.POS_MEMBER.ordinal,
        var sex: Int = SexType.MALE.ordinal,
        var at: Long = 0,
        var nick: String = "",
        var wx_nick: String = ""
) {
  fun fromUser(user: User) {
    sex = user.sex
    wx_nick = user.wx_nick
    nick = user.nick
  }
}
