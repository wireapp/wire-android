package com.wire.android.shared.asset

abstract class Asset

data class PublicAsset(val key: String) : Asset()
