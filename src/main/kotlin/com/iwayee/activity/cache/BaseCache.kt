package com.iwayee.activity.cache

import com.google.common.base.Joiner
import com.google.common.base.Splitter

open class BaseCache {
  companion object {
    var joiner: Joiner = Joiner.on(",").skipNulls()
    var splitter: Splitter = Splitter.on(",").trimResults().omitEmptyStrings()
  }
}
