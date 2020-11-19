package com.wire.android.feature.conversation.data

import androidx.lifecycle.asFlow
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.wire.android.feature.conversation.Conversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ConversationsPagingDelegate(private val scope: CoroutineScope, private val pageSize: Int) {

    fun conversationList(
        dataFactory: DataSource.Factory<Int, Conversation>,
        loadNextPage: suspend (String?, Int) -> Unit
    ): Flow<PagedList<Conversation>> {

        val boundaryCallback = object : PagedList.BoundaryCallback<Conversation>() {
            override fun onZeroItemsLoaded() {
                scope.launch { loadNextPage(null, pageSize) }
            }

            override fun onItemAtEndLoaded(itemAtEnd: Conversation) {
                scope.launch { loadNextPage(itemAtEnd.id, pageSize) }
            }
        }

        return dataFactory.toLiveData(pageSize = pageSize, boundaryCallback = boundaryCallback).asFlow()
    }
}
