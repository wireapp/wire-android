package com.wire.android.core.util

/**
 * Based on the selection of an intermediate key, adds a new layer of mapping
 * to each of the current values value.
 *
 * **Example**
 *
 * Take this map as input: {A: 1, B: 2, C: 3}
 *
 * And an intermediate key selector that returns `value + 1`.
 *
 * The result would be the following:
 *
 * {A: {2: 1}, B: {3: 2}, C: {4: 3}}
 */
inline fun <K, V, R> Map<K, V>.spread(intermediateKeySelector: (K, V) -> R): Map<K, Map<R, V>> =
    entries.fold(mutableMapOf()) { accumulator, entry ->
        accumulator[entry.key] = mapOf(intermediateKeySelector(entry.key, entry.value) to entry.value)
        accumulator
    }
