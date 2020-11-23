package com.wire.android.feature.conversation.list.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.wire.android.R
import com.wire.android.core.extension.toast
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import kotlinx.android.synthetic.main.fragment_conversation_list.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class ConversationListFragment : Fragment(R.layout.fragment_conversation_list) {

    private val viewModel: ConversationListViewModel by viewModel()
    private val conversationListAdapter by inject<ConversationListAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayUserName()
        displayConversationList()
        subscribeToEvents()
    }

    private fun displayUserName() {
        viewModel.userNameLiveData.observe(viewLifecycleOwner) {
            conversationListUserInfoTextView.text = it
        }
        viewModel.fetchUserName()
    }

    private fun displayConversationList() {
        setUpRecyclerView()
        //TODO: handle empty list
        viewModel.conversationsLiveData.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                conversationListAdapter.submitList(it)
            }.onFailure {
                showConversationListDisplayError()
            }
        }

        viewModel.fetchConversations()
    }

    private fun setUpRecyclerView() = with(conversationListRecyclerView) {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        adapter = conversationListAdapter
    }

    //TODO: check how we display errors
    private fun showConversationListDisplayError() = toast("Error while loading conversations")

    private fun subscribeToEvents() = viewModel.subscribeToEvents()
}
