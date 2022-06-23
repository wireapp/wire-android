package com.wire.android.ui.home.newconversation

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.home.newconversation.search.SearchResultState
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class NewConversationViewModelTest {

    @Test
    fun `when search with search query, return results for known and public search`() {
        runTest {
            // Given
            val (_, viewModel) = NewConversationViewModelArrangement().arrange()

            // When
            viewModel.search("search")
            advanceTimeBy(501) // 500ms debounce

            // Then
            assertEquals(
                viewModel.state.localContactSearchResult.searchResultState, SearchResultState.Success(
                    result = listOf(
                        Contact(
                            id = "knownValue",
                            domain = "domain",
                            name = "knownUsername",
                            avatarData = UserAvatarData(
                                asset = ImageAsset.UserAvatarAsset(userAssetId = UserAssetId("value", "domain")),
                                availabilityStatus = UserAvailabilityStatus.NONE
                            ),
                            label = "knownHandle",
                            connectionState = ConnectionState.NOT_CONNECTED
                        )
                    )
                )
            )

            assertEquals(
                viewModel.state.publicContactsSearchResult.searchResultState, SearchResultState.Success(
                    result = listOf(
                        Contact(
                            id = "publicValue",
                            domain = "domain",
                            name = "publicUsername",
                            avatarData = UserAvatarData(
                                asset = ImageAsset.UserAvatarAsset(userAssetId = UserAssetId("value", "domain")),
                                availabilityStatus = UserAvailabilityStatus.NONE
                            ),
                            label = "publicHandle",
                            connectionState = ConnectionState.NOT_CONNECTED
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `when search with search query, return failure for known and public search`() {
        runTest {
            // Given
            val (_, viewModel) = NewConversationViewModelArrangement()
                .withFailureKnownSearchResponse()
                .withFailurePublicSearchResponse()
                .arrange()

            // When
            viewModel.search("search")
            advanceTimeBy(501) // 500ms debounce

            // Then
            assertEquals(viewModel.state.localContactSearchResult.searchResultState is SearchResultState.Failure, true)
            assertEquals(viewModel.state.publicContactsSearchResult.searchResultState is SearchResultState.Failure, true)
        }
    }
}
