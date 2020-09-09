package com.wire.android.feature.conversation.list.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import kotlinx.android.synthetic.main.fragment_conversation_list.*
import org.koin.android.viewmodel.ext.android.viewModel

//TODO: display conversation list here
class ConversationListFragment : Fragment(R.layout.fragment_conversation_list) {

    private val viewModel: ConversationListViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeUserName()
        viewModel.fetchUserName() //TODO: acquire a Flow instance to get notified of changes.
    }

    private fun observeUserName() {
        viewModel.userNameLiveData.observe(viewLifecycleOwner) {
            conversationListWelcomeTextView.text = getString(R.string.conversation_list_welcome_text, it)
        }
    }
}
