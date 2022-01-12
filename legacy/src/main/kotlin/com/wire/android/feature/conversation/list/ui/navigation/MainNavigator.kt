package com.wire.android.feature.conversation.list.ui.navigation

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.wire.android.core.extension.clearStack
import com.wire.android.core.ui.navigation.FragmentStackHandler
import com.wire.android.feature.conversation.list.MainActivity
import com.wire.android.feature.conversation.list.ui.ConversationListFragment

class MainNavigator(private val fragmentStackHandler: FragmentStackHandler) {

    fun openMainScreen(context: Context) = context.startActivity(MainActivity.newIntent(context).clearStack())

    fun openConversationListScreen(activity: FragmentActivity, addToBackStack: Boolean = false) {
        fragmentStackHandler.replaceFragment(activity, addToBackStack = addToBackStack) {
            ConversationListFragment.newInstance()
        }
    }
}
