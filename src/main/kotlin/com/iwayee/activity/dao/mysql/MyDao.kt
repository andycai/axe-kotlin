package com.iwayee.activity.dao.mysql

import com.iwayee.activity.hub.Hub
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.PoolOptions

open class MyDao {
  protected var client: MySQLPool? = null
    get() {
      if (field == null) {
        Hub.config?.mysql?.let { cfg ->
          var connectOptions = MySQLConnectOptions()
                  .setPort(cfg.port)
                  .setHost(cfg.host)
                  .setDatabase(cfg.db)
                  .setUser(cfg.user)
                  .setPassword(cfg.password)
                  .setCharset(cfg.charset)
          // Pool option
          var poolOptions = PoolOptions()
                  .setMaxSize(cfg.pool_max)

          // Create the client pool
          field = MySQLPool.pool(Hub.vertx, connectOptions, poolOptions)
        }
      }
      return field
    }

  fun close() {
    client?.close()
  }
}
