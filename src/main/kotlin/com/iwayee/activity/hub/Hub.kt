package com.iwayee.activity.hub

import com.iwayee.activity.config.Config
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx

object Hub {
  var vertx: Vertx? = null
  var config: Config = Config()

  fun loadConfig(action: () -> Unit) {
    val retriever = ConfigRetriever.create(vertx)
    retriever.getConfig { json ->
      config.fromJson(json.result())
      action()
    }
  }
}
