package com.wire.android.core.ui.dialog

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialDialogBuilderProvider {
    fun provide(context: Context) = MaterialAlertDialogBuilder(context)
}
