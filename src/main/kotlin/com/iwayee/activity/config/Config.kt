package com.iwayee.activity.config

import io.vertx.core.json.JsonObject

class Config {
  var port: Int = 8888
  var mysql: MySQL = MySQL()
  var redis: Redis = Redis()

  fun fromJson(jo: JsonObject) {
    port = jo.getInteger("port")
    mysql = jo.getJsonObject("mysql").mapTo(MySQL::class.java)
    redis = jo.getJsonObject("redis").mapTo(Redis::class.java)
  }
}
