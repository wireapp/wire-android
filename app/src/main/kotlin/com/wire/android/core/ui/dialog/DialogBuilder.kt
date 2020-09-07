package com.wire.android.core.ui.dialog

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wire.android.R

class DialogBuilder {

    fun showDialog(context: Context, block: MaterialAlertDialogBuilder.() -> Unit) {
        MaterialAlertDialogBuilder(context).apply(block).create().show()
    }

    fun showErrorDialog(context: Context, errorMessage: ErrorMessage) = showDialog(context) {
        errorMessage.title?.let { setTitle(it) }
        setMessage(errorMessage.message)
        setPositiveButton(R.string.ok, NO_OP_LISTENER)
    }

    companion object {
        val NO_OP_LISTENER = DialogInterface.OnClickListener { _, _ -> /*no-op*/ }
    }
}
