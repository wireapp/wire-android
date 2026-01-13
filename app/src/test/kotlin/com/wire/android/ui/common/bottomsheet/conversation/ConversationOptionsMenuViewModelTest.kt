/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.common.bottomsheet.conversation

import androidx.work.WorkManager
import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.ui.home.HomeSnackBarMessage
import com.wire.android.workmanager.worker.ConversationDeletionLocallyStatus
import com.wire.android.workmanager.worker.enqueueConversationDeletionLocally
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.data.conversation.FolderType
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.delete.MarkConversationAsDeletedLocallyUseCase
import com.wire.kalium.logic.feature.conversation.delete.MarkConversationAsDeletedResult
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFolderUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.Result
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationOptionsMenuViewModelTest {
    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given success, when adding to favorites, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withAddToFavorites(AddConversationToFavoritesUseCase.Result.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.changeFavoriteState(conversationId, "name", true)

            coVerify(exactly = 1) { arrangement.addConversationToFavorites(conversationId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateFavoriteStatusSuccess>(it.message).also {
                    assertEquals(true, it.addingToFavorite)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when adding to favorites, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withAddToFavorites(AddConversationToFavoritesUseCase.Result.Failure(CoreFailure.Unknown(null)))
            .arrange()

        viewModel.actions.test {
            viewModel.changeFavoriteState(conversationId, "name", true)

            coVerify(exactly = 1) { arrangement.addConversationToFavorites(conversationId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateFavoriteStatusError>(it.message).also {
                    assertEquals(true, it.addingToFavorite)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when removing from favorites, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withRemoveFromFavorites(RemoveConversationFromFavoritesUseCase.Result.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.changeFavoriteState(conversationId, "name", false)

            coVerify(exactly = 1) { arrangement.removeConversationFromFavorites(conversationId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateFavoriteStatusSuccess>(it.message).also {
                    assertEquals(false, it.addingToFavorite)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when removing from favorites, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withRemoveFromFavorites(RemoveConversationFromFavoritesUseCase.Result.Failure(CoreFailure.Unknown(null)))
            .arrange()

        viewModel.actions.test {
            viewModel.changeFavoriteState(conversationId, "name", false)

            coVerify(exactly = 1) { arrangement.removeConversationFromFavorites(conversationId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateFavoriteStatusError>(it.message).also {
                    assertEquals(false, it.addingToFavorite)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when removing from folder, then call proper action`() = runTest(dispatcherProvider.main()) {
        val folder = ConversationFolder("folder_id", "name", FolderType.USER)
        val (arrangement, viewModel) = Arrangement()
            .withRemoveFromFolder(RemoveConversationFromFolderUseCase.Result.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.removeFromFolder(conversationId, "name", folder)

            coVerify(exactly = 1) { arrangement.removeConversationFromFolder(conversationId, folder.id) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.RemoveFromFolderSuccess>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when removing from folder, then call proper action`() = runTest(dispatcherProvider.main()) {
        val folder = ConversationFolder("folder_id", "name", FolderType.USER)
        val (arrangement, viewModel) = Arrangement()
            .withRemoveFromFolder(RemoveConversationFromFolderUseCase.Result.Failure(CoreFailure.Unknown(null)))
            .arrange()

        viewModel.actions.test {
            viewModel.removeFromFolder(conversationId, "name", folder)

            coVerify(exactly = 1) { arrangement.removeConversationFromFolder(conversationId, folder.id) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.RemoveFromFolderError>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when moving to archive, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.moveToArchive(conversationId, true, true)

            coVerify(exactly = 1) { arrangement.updateConversationArchivedStatus(conversationId, true, false, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateArchivingStatusSuccess>(it.message).also {
                    assertEquals(true, it.isArchiving)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when moving to archive, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Failure)
            .arrange()

        viewModel.actions.test {
            viewModel.moveToArchive(conversationId, true, true)

            coVerify(exactly = 1) { arrangement.updateConversationArchivedStatus(conversationId, true, false, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateArchivingStatusError>(it.message).also {
                    assertEquals(true, it.isArchiving)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when removing from archive, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.moveToArchive(conversationId, false, true)

            coVerify(exactly = 1) { arrangement.updateConversationArchivedStatus(conversationId, false, false, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateArchivingStatusSuccess>(it.message).also {
                    assertEquals(false, it.isArchiving)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when removing from archive, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Failure)
            .arrange()

        viewModel.actions.test {
            viewModel.moveToArchive(conversationId, false, true)

            coVerify(exactly = 1) { arrangement.updateConversationArchivedStatus(conversationId, false, false, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateArchivingStatusError>(it.message).also {
                    assertEquals(false, it.isArchiving)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given user is not a member, when moving to archive, then archive only locally`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.moveToArchive(conversationId, true, false)

            coVerify(exactly = 1) { arrangement.updateConversationArchivedStatus(conversationId, true, true, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateArchivingStatusSuccess>(it.message).also {
                    assertEquals(true, it.isArchiving)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given user is not a member, when removing from archive, then unarchive only locally`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.moveToArchive(conversationId, false, false)

            coVerify(exactly = 1) { arrangement.updateConversationArchivedStatus(conversationId, false, true, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UpdateArchivingStatusSuccess>(it.message).also {
                    assertEquals(false, it.isArchiving)
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when changing muted state, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateConversationMutedStatus(ConversationUpdateStatusResult.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.changeMutedState(conversationId, MutedConversationStatus.AllMuted)

            coVerify(exactly = 1) {
                arrangement.updateConversationMutedStatus(conversationId, MutedConversationStatus.AllMuted, any())
            }
            expectNoEvents() // No message expected for muting success
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when changing muted state, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUpdateConversationMutedStatus(ConversationUpdateStatusResult.Failure)
            .arrange()

        viewModel.actions.test {
            viewModel.changeMutedState(conversationId, MutedConversationStatus.AllMuted)

            coVerify(exactly = 1) {
                arrangement.updateConversationMutedStatus(conversationId, MutedConversationStatus.AllMuted, any())
            }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.MutingOperationError>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when leaving group without deleting, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withLeaveConversation(RemoveMemberFromConversationUseCase.Result.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.leaveGroup(conversationId, "name", false)

            coVerify(exactly = 1) { arrangement.leaveConversation(conversationId) }
            coVerify(exactly = 0) { arrangement.markConversationAsDeletedLocally(conversationId) }
            coVerify(exactly = 0) { arrangement.workManager.enqueueConversationDeletionLocally(conversationId, any()) }
            assertIs<ConversationOptionsMenuViewAction.Left>(awaitItem())
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.LeftConversationSuccess>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when leaving group without deleting, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withLeaveConversation(RemoveMemberFromConversationUseCase.Result.Failure(CoreFailure.Unknown(null)))
            .arrange()

        viewModel.actions.test {
            viewModel.leaveGroup(conversationId, "name", false)

            coVerify(exactly = 1) { arrangement.leaveConversation(conversationId) }
            coVerify(exactly = 0) { arrangement.markConversationAsDeletedLocally(conversationId) }
            coVerify(exactly = 0) { arrangement.workManager.enqueueConversationDeletionLocally(conversationId, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.LeaveConversationError>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when leaving group with deleting, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withLeaveConversation(RemoveMemberFromConversationUseCase.Result.Success)
            .withMarkConversationAsDeletedLocally(MarkConversationAsDeletedResult.Success)
            .withEnqueueConversationDeletionLocally(ConversationDeletionLocallyStatus.SUCCEEDED)
            .arrange()

        viewModel.actions.test {
            viewModel.leaveGroup(conversationId, "name", true)

            coVerify(exactly = 1) { arrangement.leaveConversation(conversationId) }
            coVerify(exactly = 1) { arrangement.markConversationAsDeletedLocally(conversationId) }
            coVerify(exactly = 1) { arrangement.workManager.enqueueConversationDeletionLocally(conversationId, any()) }
            assertIs<ConversationOptionsMenuViewAction.Left>(awaitItem())
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.LeftConversationSuccess>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when leaving group with deleting, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withLeaveConversation(RemoveMemberFromConversationUseCase.Result.Success)
            .withMarkConversationAsDeletedLocally(MarkConversationAsDeletedResult.Failure(CoreFailure.Unknown(null)))
            .withEnqueueConversationDeletionLocally(ConversationDeletionLocallyStatus.FAILED)
            .arrange()

        viewModel.actions.test {
            viewModel.leaveGroup(conversationId, "name", true)

            coVerify(exactly = 1) { arrangement.leaveConversation(conversationId) }
            coVerify(exactly = 1) { arrangement.markConversationAsDeletedLocally(conversationId) }
            coVerify(exactly = 0) { arrangement.workManager.enqueueConversationDeletionLocally(conversationId, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.LeaveConversationError>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when deleting group locally, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withMarkConversationAsDeletedLocally(MarkConversationAsDeletedResult.Success)
            .withEnqueueConversationDeletionLocally(ConversationDeletionLocallyStatus.SUCCEEDED)
            .arrange()

        viewModel.actions.test {
            viewModel.deleteGroupLocally(conversationId, "name")

            coVerify(exactly = 1) { arrangement.markConversationAsDeletedLocally(conversationId) }
            coVerify(exactly = 1) { arrangement.workManager.enqueueConversationDeletionLocally(conversationId, any()) }
            assertIs<ConversationOptionsMenuViewAction.DeletedLocally>(awaitItem())
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.DeleteConversationGroupLocallySuccess>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when deleting group locally, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withMarkConversationAsDeletedLocally(MarkConversationAsDeletedResult.Failure(CoreFailure.Unknown(null)))
            .withEnqueueConversationDeletionLocally(ConversationDeletionLocallyStatus.FAILED)
            .arrange()

        viewModel.actions.test {
            viewModel.deleteGroupLocally(conversationId, "name")

            coVerify(exactly = 1) { arrangement.markConversationAsDeletedLocally(conversationId) }
            coVerify(exactly = 0) { arrangement.workManager.enqueueConversationDeletionLocally(conversationId, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.DeleteConversationGroupError>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when deleting group, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withDeleteTeamConversation(Result.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.deleteGroup(conversationId, "name")

            coVerify(exactly = 1) { arrangement.deleteTeamConversation(conversationId) }
            assertIs<ConversationOptionsMenuViewAction.Deleted>(awaitItem())
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.DeletedConversationGroupSuccess>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when deleting group, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withDeleteTeamConversation(Result.Failure.NoTeamFailure)
            .arrange()

        viewModel.actions.test {
            viewModel.deleteGroup(conversationId, "name")

            coVerify(exactly = 1) { arrangement.deleteTeamConversation(conversationId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.DeleteConversationGroupError>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when blocking user, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withBlockUser(BlockUserResult.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.blockUser(userId, "name")

            coVerify(exactly = 1) { arrangement.blockUser(userId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.BlockingUserOperationSuccess>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when blocking user, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withBlockUser(BlockUserResult.Failure(CoreFailure.Unknown(null)))
            .arrange()

        viewModel.actions.test {
            viewModel.blockUser(userId, "name")

            coVerify(exactly = 1) { arrangement.blockUser(userId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.BlockingUserOperationError>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when unblocking user, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUnblockUser(UnblockUserResult.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.unblockUser(userId, "name")

            coVerify(exactly = 1) { arrangement.unblockUser(userId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UnblockingUserOperationSuccess>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when unblocking user, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withUnblockUser(UnblockUserResult.Failure(CoreFailure.Unknown(null)))
            .arrange()

        viewModel.actions.test {
            viewModel.unblockUser(userId, "name")

            coVerify(exactly = 1) { arrangement.unblockUser(userId) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.UnblockingUserOperationError>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given success, when clearing content, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withClearConversationContent(ClearConversationContentUseCase.Result.Success)
            .arrange()

        viewModel.actions.test {
            viewModel.clearConversationContent(conversationId, ConversationTypeDetail.Group.Regular(conversationId, true))

            coVerify(exactly = 1) { arrangement.clearConversationContent(conversationId, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.ClearConversationContentSuccess>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given failure, when clearing content, then call proper action`() = runTest(dispatcherProvider.main()) {
        val (arrangement, viewModel) = Arrangement()
            .withClearConversationContent(ClearConversationContentUseCase.Result.Failure(CoreFailure.Unknown(null)))
            .arrange()

        viewModel.actions.test {
            viewModel.clearConversationContent(conversationId, ConversationTypeDetail.Group.Regular(conversationId, true))

            coVerify(exactly = 1) { arrangement.clearConversationContent(conversationId, any()) }
            assertIs<ConversationOptionsMenuViewAction.Message>(awaitItem()).also {
                assertIs<HomeSnackBarMessage.ClearConversationContentFailure>(it.message)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    inner class Arrangement {
        @MockK
        lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        @MockK
        lateinit var observeSelfUser: ObserveSelfUserUseCase

        @MockK
        lateinit var addConversationToFavorites: AddConversationToFavoritesUseCase

        @MockK
        lateinit var removeConversationFromFavorites: RemoveConversationFromFavoritesUseCase

        @MockK
        lateinit var removeConversationFromFolder: RemoveConversationFromFolderUseCase

        @MockK
        lateinit var updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase

        @MockK
        lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

        @MockK
        lateinit var deleteTeamConversation: DeleteTeamConversationUseCase

        @MockK
        lateinit var markConversationAsDeletedLocally: MarkConversationAsDeletedLocallyUseCase

        @MockK
        lateinit var leaveConversation: LeaveConversationUseCase

        @MockK
        lateinit var blockUser: BlockUserUseCase

        @MockK
        lateinit var unblockUser: UnblockUserUseCase

        @MockK
        lateinit var clearConversationContent: ClearConversationContentUseCase

        @MockK
        lateinit var workManager: WorkManager

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic("com.wire.android.workmanager.worker.DeleteConversationLocallyWorkerKt")
        }

        fun arrange() = this to ConversationOptionsMenuViewModelImpl(
            currentAccount = selfUserId,
            observeConversationDetails = observeConversationDetails,
            observeSelfUser = observeSelfUser,
            addConversationToFavorites = addConversationToFavorites,
            removeConversationFromFavorites = removeConversationFromFavorites,
            removeConversationFromFolder = removeConversationFromFolder,
            updateConversationArchivedStatus = updateConversationArchivedStatus,
            updateConversationMutedStatus = updateConversationMutedStatus,
            deleteTeamConversation = deleteTeamConversation,
            markConversationAsDeletedLocally = markConversationAsDeletedLocally,
            leaveConversation = leaveConversation,
            blockUser = blockUser,
            unblockUser = unblockUser,
            clearConversationContent = clearConversationContent,
            workManager = workManager,
            dispatchers = dispatcherProvider,
        )

        fun withAddToFavorites(result: AddConversationToFavoritesUseCase.Result) = apply {
            coEvery { addConversationToFavorites(any()) } returns result
        }

        fun withRemoveFromFavorites(result: RemoveConversationFromFavoritesUseCase.Result) = apply {
            coEvery { removeConversationFromFavorites(any()) } returns result
        }

        fun withRemoveFromFolder(result: RemoveConversationFromFolderUseCase.Result) = apply {
            coEvery { removeConversationFromFolder(any(), any()) } returns result
        }

        fun withUpdateConversationMutedStatus(result: ConversationUpdateStatusResult) = apply {
            coEvery { updateConversationMutedStatus(any(), any(), any()) } returns result
        }

        fun withBlockUser(result: BlockUserResult) = apply {
            coEvery { blockUser(any()) } returns result
        }

        fun withUnblockUser(result: UnblockUserResult) = apply {
            coEvery { unblockUser(any()) } returns result
        }

        fun withDeleteTeamConversation(result: Result) = apply {
            coEvery { deleteTeamConversation(any()) } returns result
        }

        fun withLeaveConversation(result: RemoveMemberFromConversationUseCase.Result) = apply {
            coEvery { leaveConversation(any()) } returns result
        }

        fun withMarkConversationAsDeletedLocally(result: MarkConversationAsDeletedResult) = apply {
            coEvery { markConversationAsDeletedLocally(any()) } returns result
        }

        fun withEnqueueConversationDeletionLocally(result: ConversationDeletionLocallyStatus) = apply {
            coEvery { workManager.enqueueConversationDeletionLocally(any(), any()) } returns flowOf(result)
        }

        fun withUpdateArchivedStatus(result: ArchiveStatusUpdateResult) = apply {
            coEvery { updateConversationArchivedStatus(any(), any(), any()) } returns result
            coEvery { updateConversationArchivedStatus(any(), any(), any(), any()) } returns result
        }

        fun withClearConversationContent(result: ClearConversationContentUseCase.Result) = apply {
            coEvery { clearConversationContent(any(), any()) } returns result
        }
    }

    companion object {
        private val conversationId = ConversationId("some_id", "some_domain")
        private val userId: UserId = UserId("someUser", "some_domain")
        private val selfUserId: UserId = UserId("selfUser", "some_domain")
    }
}
