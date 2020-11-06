package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.list.usecase.Conversation

class ConversationListAdapter(
    private val conversationList: List<Conversation>,
    private val viewHolderInflater: ViewHolderInflater = ViewHolderInflater()
) : RecyclerView.Adapter<ConversationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder =
        ConversationViewHolder(parent, viewHolderInflater)

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) =
        holder.bind(conversationList[position])

    override fun getItemCount(): Int = conversationList.size
}
