package com.wire.android.ui.home

import io.github.esentsov.PackagePrivate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeCommonManager @Inject constructor() {

    var scrollBridge: HomeScrollBridge? = null
        private set

    @PackagePrivate // should be used only from HomeViewModel
    fun onViewModelInit() {
        scrollBridge = HomeScrollBridge()
    }

    @PackagePrivate // should be used only from HomeViewModel
    fun onViewModelCleared() {
        scrollBridge = null
    }
}
