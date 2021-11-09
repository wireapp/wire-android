package com.wire.android.feature.conversation.content.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R
import com.wire.android.feature.conversation.ConversationID
import kotlinx.android.synthetic.main.activity_conversation.*
import org.koin.android.viewmodel.ext.android.viewModel

class ConversationActivity : AppCompatActivity(R.layout.activity_conversation) {

    private var _conversationId: ConversationID? = null
    private val conversationId get() = _conversationId!!
    private val conversationTitle get() = intent.getStringExtra(ARG_CONVERSATION_TITLE)
    private val viewModel by viewModel<ConversationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conversationIdValue = intent.getStringExtra(ARG_CONVERSATION_ID)
        val conversationDomain = intent.getStringExtra(ARG_CONVERSATION_DOMAIN)
        if (conversationDomain != null && conversationIdValue != null) {
            _conversationId = ConversationID(conversationIdValue, conversationDomain)
        }
        setUpConversationTitle()
        setUpBackNavigation()
        cacheConversationId()
    }

    private fun cacheConversationId() {
        viewModel.cacheConversationId(conversationId)
    }

    private fun setUpBackNavigation() {
        conversationToolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setUpConversationTitle() {
        setSupportActionBar(conversationToolbar)
        conversationToolbarTitleTextView.text = conversationTitle
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    companion object {
        fun newIntent(context: Context, conversationIdValue: String, conversationDomain: String, conversationTitle: String) =
            Intent(context, ConversationActivity::class.java).apply {
                putExtra(ARG_CONVERSATION_ID, conversationIdValue)
                putExtra(ARG_CONVERSATION_DOMAIN, conversationDomain)
                putExtra(ARG_CONVERSATION_TITLE, conversationTitle)
            }

        private const val ARG_CONVERSATION_ID = "conversation-id-arg"
        private const val ARG_CONVERSATION_DOMAIN = "conversation-domain-arg"
        private const val ARG_CONVERSATION_TITLE = "conversation-title-arg"
    }
}
