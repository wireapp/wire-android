/* Wire Copyright (C) 2026 Wire Swiss GmbH */
@file:Suppress("Filename", "MatchingDeclarationName")

package com.wire.android.ui.authentication.login

enum class LoginSurface { Verification, Main }

fun loginSurface(verificationRequired: Boolean): LoginSurface =
    if (verificationRequired) LoginSurface.Verification else LoginSurface.Main
