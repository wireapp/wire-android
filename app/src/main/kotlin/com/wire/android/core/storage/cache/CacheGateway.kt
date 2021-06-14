package com.wire.android.core.storage.cache

import com.wire.android.core.extension.EMPTY

class CacheGateway {
    private val data = HashMap<Any, Any>()

    fun save(key: String, value: Any) { data[key] = value }
    fun load(key: Any): Any = data[key] ?: String.EMPTY
}
