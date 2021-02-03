package com.wire.android.feature.conversation.list.ui

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.wire.android.R
import com.wire.android.core.extension.toast
import com.wire.android.feature.conversation.list.toolbar.ToolbarData
import com.wire.android.feature.conversation.list.toolbar.ui.icon.ToolbarProfileIcon
import com.wire.android.shared.user.User
import kotlinx.android.synthetic.main.fragment_conversation_list.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class ConversationListFragment : Fragment(R.layout.fragment_conversation_list) {

    private val viewModel by viewModel<ConversationListViewModel>()
    private val conversationListAdapter by inject<ConversationListAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayToolbar()
        displayConversationList()
        subscribeToEvents()
    }

    private fun displayToolbar() {
        viewModel.toolbarDataLiveData.observe(viewLifecycleOwner) {
            displayUserName(it.user)
            displayProfileIcon(it)
        }
        viewModel.fetchToolbarData()
    }

    private fun displayUserName(user: User) {
        conversationListUserInfoTextView.text = user.name
    }

    private fun displayProfileIcon(toolbarData: ToolbarData) = toolbarData.run {
        val toolbarProfileIcon =
            if (team != null) ToolbarProfileIcon.forTeam(team)
            else ToolbarProfileIcon.forUser(user)

        toolbarProfileIcon.displayOn(conversationListProfileIconImageView)
    }

    private fun displayConversationList() {
        setUpRecyclerView()
        //TODO: handle empty list
        viewModel.conversationListItemsLiveData.observe(viewLifecycleOwner) {
            conversationListAdapter.submitList(it)
        }
    }

    private fun setUpRecyclerView() = with(conversationListRecyclerView) {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        adapter = conversationListAdapter

        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
            ContextCompat.getDrawable(context, R.drawable.conversation_list_divider)?.let { setDrawable(it) }
        }
        addItemDecoration(divider)
    }

    //TODO: check how we display errors
    private fun showConversationListDisplayError() = toast("Error while loading conversations")

    private fun subscribeToEvents() = viewModel.subscribeToEvents()
}
