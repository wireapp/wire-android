/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.avatarpicker

internal fun String?.toAvatarPickerResult() = AvatarPickerResult(assetId = this)

internal fun AvatarPickerResult.toLegacyResult(): String? = assetId
