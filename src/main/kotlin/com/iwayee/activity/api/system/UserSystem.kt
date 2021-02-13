package com.iwayee.activity.api.system

import com.iwayee.activity.cache.UserCache
import com.iwayee.activity.define.ErrCode
import com.iwayee.activity.hub.Some
import io.vertx.ext.web.RoutingContext

object UserSystem {

  fun login(ctx : RoutingContext) {
    ctx.end("hi, andy login")
  }

  fun getUser(some: Some) {
    var uid = some.getUInt("uid");

    UserCache.getUserById(uid) {
      it?.let {
        some.ok(it)
        return@getUserById
      }
      some.err(ErrCode.ERR_DATA)
    }
  }
}
