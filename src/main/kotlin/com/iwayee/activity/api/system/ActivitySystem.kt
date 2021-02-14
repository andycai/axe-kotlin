package com.iwayee.activity.api.system

import com.iwayee.activity.api.comp.Activity
import com.iwayee.activity.api.comp.Group
import com.iwayee.activity.hub.Some
import io.vertx.core.json.JsonObject

object ActivitySystem {
  private fun doCreate(some: Some, jo: JsonObject, uid: Int, group: Group) {
    //
  }

  fun create(some: Some) {
    //
  }

  fun getActivitiesByUserId(some: Some) {
    //
  }

  fun getActivitiesByGroupId(some: Some) {
    //
  }

  fun getActivities(some: Some) {
    //
  }

  fun getActivityById(some: Some) {
    //
  }

  private fun doUpdate(some: Some, act: Activity) {
    //
  }

  fun update(some: Some) {
    //
  }

  private fun doEnd(some: Some, aid: Int, jo: JsonObject) {
    //
  }

  fun endActivity(some: Some) {
    //
  }

  private fun enqueue(some: Some, uid: Int, act: Activity, maleCount: Int, femaleCount: Int) {
    //
  }

  fun applyActivity(some: Some) {
    //
  }

  private fun dequeue(some: Some, uid: Int, act: Activity, maleCount: Int, femaleCount: Int) {
    //
  }

  fun cancelActivity(some: Some) {
    //
  }
}
