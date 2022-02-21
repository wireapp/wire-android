package com.wire.android.model

import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wire.kalium.logic.data.id.QualifiedID
import kotlinx.parcelize.Parcelize

typealias ConversationId = QualifiedIDReference
typealias UserId = QualifiedIDReference

@Parcelize
data class QualifiedIDReference(
    val value: String,
    val domain: String
) : Parcelable {

    fun toBaseID() = QualifiedID(value, domain)
    fun toJson(): String = Gson().toJson(this)
}

class QualifiedIDAssetParamType : NavType<QualifiedIDReference>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): QualifiedIDReference? {
        return bundle.getParcelable(key)
    }

    override fun parseValue(value: String): QualifiedIDReference {
        return Gson().fromJson(value, object : TypeToken<QualifiedIDReference>() {}.type)
    }

    override fun put(bundle: Bundle, key: String, value: QualifiedIDReference) {
        bundle.putParcelable(key, value)
    }
}

fun QualifiedID.toLocalID() = QualifiedIDReference(value, domain)
