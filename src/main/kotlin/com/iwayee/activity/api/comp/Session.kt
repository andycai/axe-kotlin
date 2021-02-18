package com.iwayee.activity.api.comp

import com.iwayee.activity.define.SexType

data class Session(
        var uid: Long = 0,
        var sex: Int = SexType.MALE.ordinal,
        var at: Long = 0,
        var token: String = ""
)
