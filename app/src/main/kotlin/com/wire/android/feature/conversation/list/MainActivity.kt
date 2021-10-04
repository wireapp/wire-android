package com.wire.android.feature.conversation.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkInfo
import com.wire.android.R
import com.wire.android.core.ui.navigation.Navigator
import com.wire.android.feature.sync.ui.SyncViewModel
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val syncViewModel by viewModel<SyncViewModel>()

    private val navigator by inject<Navigator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncData()
        setUpBottomNavigation()
    }

    private fun syncData() {
        syncViewModel.startSync() //TODO: this should normally be triggered by PushService
        syncViewModel.startListeningToMessages()
        syncViewModel.syncStatusLiveData.observe(this) {
            handleLoadingBarVisibility(it)
        }
    }

    private fun handleLoadingBarVisibility(state: WorkInfo.State) {
        if (state == WorkInfo.State.RUNNING)
            mainLoadingProgressIndicator.visibility = View.VISIBLE
        else
            mainLoadingProgressIndicator.visibility = View.GONE
    }

    private fun setUpBottomNavigation() = with(mainBottomNavigation) {
        setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.main_search_users -> handleSearchUsersMenuClick()
                R.id.main_conversations -> handleConversationsMenuClick()
                else -> false
            }
        }

        selectedItemId = R.id.main_conversations
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun handleSearchUsersMenuClick(): Boolean {
        //TODO: open user search screen (and return true here)
        return false
    }

    private fun handleConversationsMenuClick(): Boolean {
        navigator.main.openConversationListScreen(this)
        return true
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
