package com.wire.android.core.util

/**
 * Based on the selection of an intermediate key, adds a new layer of mapping.
 * For example, take the following map as input [A: [1,2], B: [2], C: [3]]
 * And an intermediate key selection that returns true if the value contains an even number.
 * The result would be:
 * [A: [true: [1,2]], B: [true: [2], C: [false: [3]]]
 *
 */
inline fun <K, V, R> Map<K, V>.spread(groupKeySelector: (K, V) -> R): Map<K, Map<R, V>> {
    val newMap = mutableMapOf<K, MutableMap<R, V>>()

    this.forEach { entry ->
        val intermediateKey = groupKeySelector(entry.key, entry.value)
        newMap[entry.key] = newMap[entry.key] ?: mutableMapOf()
        newMap[entry.key]!![intermediateKey] = entry.value
    }
    return newMap
}