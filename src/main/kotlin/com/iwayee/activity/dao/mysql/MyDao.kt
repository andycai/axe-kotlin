package com.iwayee.activity.dao.mysql

import com.iwayee.activity.hub.Hub
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.sqlclient.PoolOptions

open class MyDao {
  protected var client: MySQLPool? = null
    get() {
      if (field == null) {
        var connectOptions = MySQLConnectOptions()
          .setPort(Hub.config!!.mysql.port)
          .setHost(Hub.config!!.mysql.host)
          .setDatabase(Hub.config!!.mysql.db)
          .setUser(Hub.config!!.mysql.user)
          .setPassword(Hub.config!!.mysql.password)
          .setCharset(Hub.config!!.mysql.charset)

        // Pool option
        var poolOptions = PoolOptions()
          .setMaxSize(Hub.config!!.mysql.pool_max)

        // Create the client pool
        field = MySQLPool.pool(Hub.vertx, connectOptions, poolOptions)
      }
      return field
    }

  fun close() {
    client?.close()
  }
}
