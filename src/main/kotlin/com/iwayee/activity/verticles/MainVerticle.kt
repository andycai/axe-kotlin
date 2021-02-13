package com.iwayee.activity.verticles

import com.iwayee.activity.api.system.UserSystem
import com.iwayee.activity.hub.Hub
import com.iwayee.activity.hub.Some
import com.iwayee.activity.utils.TokenExpiredException
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.lang.IllegalArgumentException

class MainVerticle: AbstractVerticle() {
  private var router: Router? = null

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
        // some.checkToken()
      }
      action(some)
    } catch (e: IllegalArgumentException) {
      //
    } catch (e: TokenExpiredException) {
      //
    }
  }

  private fun get(s: String, action: (Some) -> Unit, auth: Boolean = true) {
    router?.get(s)?.handler{ctx ->
      runAction(ctx, action, auth)
    }
  }

  fun startServer() {
    router = Router.router(vertx)
    router?.route()?.handler(BodyHandler.create())

    router?.get("/login")?.handler(UserSystem::login)

    get("/users/:uid", UserSystem::getUser)

    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(8888)
      .onSuccess{ server ->
        println("HTTP server started on port ${server.actualPort()}")
      }
//      .requestHandler { req ->
//        req.response()
//          .putHeader("content-type", "text/plain")
//          .end("Hello from Vert.x!")
//      }
//      .listen(8888) { http ->
//        if (http.succeeded()) {
//          startPromise.complete()
//          println("HTTP server started on port 8888")
//        } else {
//          startPromise.fail(http.cause());
//        }
//      }
  }
}
