package com.wire.android.core.ui.dialog

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wire.android.R

class DialogBuilder(private val dialogBuilderProvider: MaterialDialogBuilderProvider) {

    fun showDialog(context: Context, block: MaterialAlertDialogBuilder.() -> Unit) {
        dialogBuilderProvider.provide(context).apply(block).create().show()
    }

    fun showErrorDialog(
        context: Context,
        errorMessage: ErrorMessage,
        isCancelable: Boolean = true,
        clickListener: DialogInterface.OnClickListener = NO_OP_LISTENER
    ) =
        showDialog(context) {
            setCancelable(isCancelable)
            errorMessage.title?.let { setTitle(it) }
            setMessage(errorMessage.message)
            setPositiveButton(R.string.ok, clickListener)
        }

    companion object {
        val NO_OP_LISTENER = DialogInterface.OnClickListener { _, _ -> /*no-op*/ }
    }
}
