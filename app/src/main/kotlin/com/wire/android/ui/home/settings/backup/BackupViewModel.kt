package com.wire.android.ui.home.settings.backup

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor() : ViewModel() {

    init{
        Log.d("TEST","this is test of init")
    }

    override fun onCleared() {
        super.onCleared()
    }

}
