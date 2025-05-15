package com.wire.android.tests.core.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
open class MessageInfo(
    var mailbox: String? = null,
    var id: String? = null,
    var from: String? = null,
    var to: List<String>? = null,
    var date: String? = null,
    var subject: String? = null,
    var size: Int = 0
)
