/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AnyPrimitiveAsStringSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = AnyPrimitiveSurrogate.serializer().descriptor
    override fun serialize(encoder: Encoder, value: Any) =
        encoder.encodeSerializableValue(AnyPrimitiveSurrogate.serializer(), AnyPrimitiveSurrogate(value))
    override fun deserialize(decoder: Decoder): Any =
        decoder.decodeSerializableValue(AnyPrimitiveSurrogate.serializer()).value
}

@Serializable
@SerialName("AnyPrimitive")
private data class AnyPrimitiveSurrogate(private val kind: AnyPrimitiveKind, private val stringValue: String) {
    constructor(value: Any) : this(
        kind = when (value) {
            is String -> AnyPrimitiveKind.STRING
            is Int -> AnyPrimitiveKind.INT
            is Long -> AnyPrimitiveKind.LONG
            is Float -> AnyPrimitiveKind.FLOAT
            is Double -> AnyPrimitiveKind.DOUBLE
            is Boolean -> AnyPrimitiveKind.BOOLEAN
            else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
        },
        stringValue = value.toString()
    )

    val value: Any
        get() = when (kind) {
            AnyPrimitiveKind.STRING -> stringValue
            AnyPrimitiveKind.INT -> stringValue.toInt()
            AnyPrimitiveKind.LONG -> stringValue.toLong()
            AnyPrimitiveKind.FLOAT -> stringValue.toFloat()
            AnyPrimitiveKind.DOUBLE -> stringValue.toDouble()
            AnyPrimitiveKind.BOOLEAN -> stringValue.toBoolean()
        }
}

private enum class AnyPrimitiveKind { STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN }
