/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.content.external

import android.net.Uri

fun ExternalContentReference.asAndroidUri(): Uri = Uri.parse(token)

fun Uri.asExternalContentReference(): ExternalContentReference = ExternalContentReference(toString())
