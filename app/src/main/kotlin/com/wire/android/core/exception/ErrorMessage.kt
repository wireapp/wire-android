package com.wire.android.core.exception

import androidx.annotation.StringRes

//TODO: A more generic String/StringRes sharing mechanism for VM & View
data class ErrorMessage(@StringRes val message: Int)
