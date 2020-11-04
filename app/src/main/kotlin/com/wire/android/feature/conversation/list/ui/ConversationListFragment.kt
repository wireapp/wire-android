package com.wire.android.feature.conversation.list.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.wire.android.R
import com.wire.android.core.extension.toast
import com.wire.android.core.flags.FeatureFlag
import com.wire.android.core.flags.Flag
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import kotlinx.android.synthetic.main.fragment_conversation_list.*
import org.koin.android.viewmodel.ext.android.viewModel

//TODO: UI test
class ConversationListFragment : Fragment(R.layout.fragment_conversation_list) {

    private val viewModel: ConversationListViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayUserName()
        Flag.Conversations whenActivated { displayConversationList() }
    }

    private fun displayUserName() {
        viewModel.userNameLiveData.observe(viewLifecycleOwner) {
            conversationListUserInfoTextView.text = it
        }
        viewModel.fetchUserName() //TODO: acquire a Flow instance to get notified of changes.
    }

    private fun displayConversationList() = with(conversationListRecyclerView) {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)

        //TODO: handle empty list
        viewModel.conversationsLiveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                adapter = ConversationListAdapter(it) //TODO: just update the data
            }.onFailure {
                showConversationListDisplayError()
            }
        }
        viewModel.fetchConversations()
    }

    //TODO: implement
    private fun showConversationListDisplayError() = toast("Error while loading conversations")
}
