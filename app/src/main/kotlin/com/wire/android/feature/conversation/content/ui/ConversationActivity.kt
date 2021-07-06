package com.wire.android.feature.conversation.content.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R
import kotlinx.android.synthetic.main.activity_conversation.*

class ConversationActivity : AppCompatActivity(R.layout.activity_conversation) {

    private val conversationTitle get() = intent.getStringExtra(ARG_CONVERSATION_TITLE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpConversationTitle()
        setUpBackNavigation()
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
        fun newIntent(context: Context, conversationId: String, conversationTitle: String) =
            Intent(context, ConversationActivity::class.java).apply {
                putExtra(ARG_CONVERSATION_ID, conversationId)
                putExtra(ARG_CONVERSATION_TITLE, conversationTitle)
            }
        private const val ARG_CONVERSATION_ID = "conversation-id-arg"
        private const val ARG_CONVERSATION_TITLE = "conversation-title-arg"
    }
}
