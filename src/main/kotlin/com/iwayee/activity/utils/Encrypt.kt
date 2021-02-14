package com.iwayee.activity.utils

import com.google.common.hash.Hashing

object Encrypt {
  fun md5(s: String): String {
    return Hashing.md5().newHasher().putString(s, Charsets.UTF_8).hash().toString()
  }

  fun sha1(s: String): String {
    return Hashing.sha1().newHasher().putString(s, Charsets.UTF_8).hash().toString()
  }

  fun sha256(s: String): String {
    return Hashing.sha256().newHasher().putString(s, Charsets.UTF_8).hash().toString()
  }
}