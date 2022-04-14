package com.wire.android.ui.common

import androidx.lifecycle.ViewModel
import com.wire.android.util.ui.WireSessionImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CurrentSessionViewModel @Inject constructor(
    val wireSessionImageLoader: WireSessionImageLoader
): ViewModel()
