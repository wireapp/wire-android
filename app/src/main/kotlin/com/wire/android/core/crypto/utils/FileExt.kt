package com.wire.android.core.crypto.utils

import java.io.File

operator fun File.plus(subPath: String) = File(this, subPath)
