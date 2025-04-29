package com.wire.android.util

import com.wire.android.ui.markdown.isNotBlank
import com.wire.android.ui.markdown.toMarkdownDocument

fun CharSequence.isNotMarkdownBlank(): Boolean = this.isNotBlank() && this.toString().toMarkdownDocument().isNotBlank()
