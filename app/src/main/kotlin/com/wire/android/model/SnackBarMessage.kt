package com.wire.android.model

import com.wire.android.util.ui.UIText

/**
 * Interface for passing messages from view models to snackBar
 */
interface SnackBarMessage {
    val uiText: UIText
}
