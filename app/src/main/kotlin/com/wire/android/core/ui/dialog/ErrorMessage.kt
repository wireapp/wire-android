package com.wire.android.core.ui.dialog

import androidx.annotation.StringRes
import com.wire.android.R

open class ErrorMessage {
    @StringRes
    val message: Int

    @StringRes
    val title: Int?

    constructor(@StringRes message: Int) {
        this.message = message
        this.title = null
    }

    constructor(@StringRes title: Int, @StringRes message: Int) {
        this.title = title
        this.message = message
    }

    companion object {
        val EMPTY = ErrorMessage(0)
    }
}

object NetworkErrorMessage : ErrorMessage(R.string.network_error_dialog_title, R.string.network_error_dialog_message)
object GeneralErrorMessage : ErrorMessage(R.string.general_error_dialog_title, R.string.general_error_dialog_message)

