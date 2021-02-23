package com.iwayee.activity.utils

import java.util.*

object CollectionUtils {
  fun <T> subLastList(list: List<T>, page: Int, num: Int): List<T> {
    val emptyList = ArrayList<T>()
    if (Objects.isNull(list) || list.isEmpty()) return emptyList

    val offset = (page - 1) * num
    val to = list.size - offset
    if (to <= 0) return emptyList

    var from = to - num
    if (from < 0) from = 0
    return list.subList(from, to)
  }
}