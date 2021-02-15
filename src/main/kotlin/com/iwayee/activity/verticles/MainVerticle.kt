package com.iwayee.activity.verticles

import com.iwayee.activity.api.system.ActivitySystem
import com.iwayee.activity.api.system.GroupSystem
import com.iwayee.activity.api.system.UserSystem
import com.iwayee.activity.define.ErrCode
import com.iwayee.activity.hub.Hub
import com.iwayee.activity.hub.Some
import com.iwayee.activity.utils.TokenExpiredException
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.lang.IllegalArgumentException

class MainVerticle: AbstractVerticle() {
  private var router: Router? = null

  private fun json(ctx: RoutingContext, code: ErrCode) {
    ctx.json(JsonObject().put("code", code.errorCode).put("msg", code.errorDesc))
  }

  private fun errAuth(ctx: RoutingContext) {
    var code = ErrCode.ERR_AUTH
    json(ctx, code)
  }

  private fun errArg(ctx: RoutingContext) {
    var code = ErrCode.ERR_PARAM
    json(ctx, code)
  }

  override fun start(startPromise: Promise<Void>) {
    Hub.vertx = vertx
    Hub.loadConfig {
      startServer()
    }
  }

  private fun runAction(ctx: RoutingContext, action: (Some) -> Unit, auth: Boolean = true) {
    try {
      var some = Some(ctx)
      if (auth) {
         some.checkToken()
      }
      action(some)
    } catch (e: IllegalArgumentException) {
      errArg(ctx)
    } catch (e: TokenExpiredException) {
      errAuth(ctx)
    }
  }

  private fun get(s: String, action: (Some) -> Unit, auth: Boolean = true) {
    router?.get(s)?.handler{ctx ->
      runAction(ctx, action, auth)
    }
  }

  private fun post(s: String, action: (Some) -> Unit, auth: Boolean = true) {
    router?.post(s)?.handler{ctx ->
      runAction(ctx, action, auth)
    }
  }

  private fun put(s: String, action: (Some) -> Unit, auth: Boolean = true) {
    router?.put(s)?.handler{ctx ->
      runAction(ctx, action, auth)
    }
  }

  private fun delete(s: String, action: (Some) -> Unit, auth: Boolean = true) {
    router?.delete(s)?.handler{ctx ->
      runAction(ctx, action, auth)
    }
  }

  private fun startServer() {
    router = Router.router(vertx)
    router?.route()?.handler(BodyHandler.create())

    // 用户
    get("/users/:uid", UserSystem::getUser);
    get("/users/your/groups", GroupSystem::getGroupsByUserId);
    get("/users/your/activities", ActivitySystem::getActivitiesByUserId);

    post("/login", UserSystem::login, false);
    post("/login_wx", UserSystem::wxLogin, false);
    post("/register", UserSystem::register, false);
    post("/logout", UserSystem::logout);

    // 群组
    get("/groups/:gid", GroupSystem::getGroupById);
    get("/groups", GroupSystem::getGroups);
    get("/groups/:gid/pending", GroupSystem::getApplyList);
    get("/groups/:gid/activities", ActivitySystem::getActivitiesByGroupId);

    post("/groups", GroupSystem::create);
    post("/groups/:gid/apply", GroupSystem::apply);
    post("/groups/:gid/approve", GroupSystem::approve);
    post("/groups/:gid/promote/:mid", GroupSystem::promote);
    post("/groups/:gid/transfer/:mid", GroupSystem::transfer);

    put("/groups/:gid", GroupSystem::updateGroup);

    // 活动
    get("/activities/:aid", ActivitySystem::getActivityById);
    get("/activities", ActivitySystem::getActivities);

    post("/activities", ActivitySystem::create);
    post("/activities/:aid/end", ActivitySystem::end);
    post("/activities/:aid/apply", ActivitySystem::apply);
    post("/activities/:aid/cancel", ActivitySystem::cancel);

    put("/activities/:aid", ActivitySystem::update);

    Hub.config?.let {
      vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(it.port)
        .onSuccess{ server ->
          println("HTTP server started on port ${server.actualPort()}")
        }
    }
  }
}
