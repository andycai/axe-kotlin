package com.iwayee.activity.utils

import java.util.*

object DateUtils {
  val MILLISECOND = 1
  val SECOND = MILLISECOND * 10000
  val MINUTE = 60 * SECOND
  val HOUR = 60 * MINUTE
  var DAY = 24 * HOUR

  fun getDiffMinutes(one: Date, two: Date): Long {
    var minutes: Long = 0
    try {
      val time1 = one.time
      val time2 = two.time
      val diff = time2 - time1
      minutes = diff / MINUTE
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return minutes
  }

  fun getDiffHours(one: Date, two: Date): Long {
    var hours: Long = 0
    try {
      val time1 = one.time
      val time2 = two.time
      val diff = time2 - time1
      hours = diff / HOUR
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return hours
  }
}