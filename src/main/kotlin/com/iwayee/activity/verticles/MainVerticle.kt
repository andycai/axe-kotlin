package com.iwayee.activity.verticles

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
//         some.checkToken()
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

    router?.get("/login")?.handler(UserSystem::login)

    get("/users/:uid", UserSystem::getUser)

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
