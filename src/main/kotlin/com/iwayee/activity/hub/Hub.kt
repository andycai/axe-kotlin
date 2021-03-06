package com.iwayee.activity.hub

import com.iwayee.activity.config.Config
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx

object Hub {
  var vertx: Vertx? = null
  var config: Config? = null

  fun loadConfig(action: () -> Unit) {
    val retriever = ConfigRetriever.create(vertx)
    retriever.getConfig { json ->
      if (config == null) {
        config = Config()
      }
      config?.fromJson(json.result())
      action()
    }
  }
}
