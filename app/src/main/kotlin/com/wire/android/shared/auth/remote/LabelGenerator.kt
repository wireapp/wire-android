package com.wire.android.shared.auth.remote

import java.util.UUID

class LabelGenerator {
    fun newLabel() = UUID.randomUUID().toString()
}
