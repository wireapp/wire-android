package com.wire.android.core.extension

import java.io.File

operator fun File.plus(subPath: String) = File(this, subPath)
