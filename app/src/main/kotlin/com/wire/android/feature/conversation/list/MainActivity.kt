package com.wire.android.feature.conversation.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R
import com.wire.android.core.extension.toast
import com.wire.android.feature.sync.ui.SyncViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val syncViewModel by viewModel<SyncViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncData()
    }

    private fun syncData() {
        syncViewModel.startSync() //TODO: this should normally be triggered by PushService
        syncViewModel.syncStatusLiveData.observe(this) {
            //TODO: show loading bar
            toast("Syncing data: $it")
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
