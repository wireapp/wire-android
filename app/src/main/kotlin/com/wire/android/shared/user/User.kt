package com.wire.android.shared.user

import com.wire.android.core.extension.EMPTY

data class User(val id : String, val name: String, val email: String? = null) {
    companion object {
        val EMPTY = User(String.EMPTY, String.EMPTY, null)
    }
}
