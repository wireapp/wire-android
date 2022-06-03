package com.wire.android.config

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

fun mockUri(
    query: String = "query",
    queryParameterNames: Set<String> = setOf("parameterName"),
    parameterName: String = "parameterName"
) {
    val mockUri = mockk<Uri>()
    every { mockUri.query } returns "query"
    every { mockUri.queryParameterNames } returns queryParameterNames
    every { mockUri.getQueryParameter(any()) } returns parameterName
    mockkStatic(Uri::class)
    every { Uri.parse(any()) } returns mockUri
}
