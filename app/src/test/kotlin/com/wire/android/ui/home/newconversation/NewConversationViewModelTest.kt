package com.wire.android.ui.home.newconversation

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.search.SearchResultState
import com.wire.android.ui.home.conversations.search.SearchResultTitle
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionState
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class NewConversationViewModelTest {

    @Test
    fun `when search with search query, return results for known and public search`() {
        runTest {
            // Given
            val (arrangement, viewModel) = NewConversationViewModelArrangement().arrange()

            // When
            viewModel.searchQueryChanged(TextFieldValue("search"))
            advanceUntilIdle() // 500ms debounce

            // Then
            assertEquals(
                viewModel.state.searchResult[SearchResultTitle(R.string.label_contacts)]!!.searchResultState,
                SearchResultState.Success(
                    result = listOf(
                        Contact(
                            id = "knownValue",
                            domain = "domain",
                            name = "knownUsername",
                            avatarData = UserAvatarData(
                                asset = ImageAsset.UserAvatarAsset(
                                    arrangement.wireSessionImageLoader,
                                    UserAssetId("value", "domain")
                                ),
                                availabilityStatus = UserAvailabilityStatus.AVAILABLE
                            ),
                            label = "knownHandle",
                            connectionState = ConnectionState.NOT_CONNECTED,
                            membership = Membership.Federated
                        )
                    )
                )
            )

            assertEquals(
                viewModel.state.searchResult[SearchResultTitle(R.string.label_public_wire)]!!.searchResultState,
                SearchResultState.Success(
                    result = listOf(
                        Contact(
                            id = "publicValue",
                            domain = "domain",
                            name = "publicUsername",
                            avatarData = UserAvatarData(
                                asset = ImageAsset.UserAvatarAsset(
                                    arrangement.wireSessionImageLoader,
                                    UserAssetId("value", "domain")
                                ),
                                availabilityStatus = UserAvailabilityStatus.AVAILABLE
                            ),
                            label = "publicHandle",
                            connectionState = ConnectionState.NOT_CONNECTED,
                            membership = Membership.Federated
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `given sync failure, when creating group, then should update options state with connectivity error`() = runTest {
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withSyncFailureOnCreatingGroup()
            .arrange()

        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.groupOptionsState.error shouldBeEqualTo GroupOptionState.Error.LackingConnection
    }

    @Test
    fun `given unknown failure, when creating group, then should update options state with unknown error`() = runTest {
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withUnknownFailureOnCreatingGroup()
            .arrange()

        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.groupOptionsState.error shouldBeEqualTo GroupOptionState.Error.Unknown
    }

    @Test
    fun `given no failure, when creating group, then options state should have no error`() = runTest {
        val (_, viewModel) = NewConversationViewModelArrangement().arrange()

        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.groupOptionsState.error.shouldBeNull()
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
            viewModel.searchQueryChanged(TextFieldValue("search"))
            advanceUntilIdle() // 500ms debounce

            // Then
            assertEquals(
                viewModel.state.searchResult[SearchResultTitle(R.string.label_contacts)]?.searchResultState is SearchResultState.Failure,
                true
            )
            assertEquals(
                viewModel.state.searchResult[SearchResultTitle(R.string.label_public_wire)]?.searchResultState is SearchResultState.Failure,
                true
            )
        }
    }
}
