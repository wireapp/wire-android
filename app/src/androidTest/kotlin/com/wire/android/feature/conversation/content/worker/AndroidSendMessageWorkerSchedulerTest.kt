package com.wire.android.feature.conversation.content.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.wire.android.InstrumentationTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class AndroidSendMessageWorkerSchedulerTest : InstrumentationTest() {

    private lateinit var subject: AndroidSendMessageWorkerScheduler

    @Before
    fun setup() {
        subject = AndroidSendMessageWorkerScheduler(appContext)
    }

    @Test
    fun scheduleMessageSendingWorker_MessageIdAndSenderId_CorrectOneTimeRequestShouldBeBuild() {
        mockkStatic(WorkManager::class) {
            val workManager: WorkManager = mockk()
            every { WorkManager.getInstance(any()) } returns workManager
            val oneTimeRequestSlot = slot<OneTimeWorkRequest>()
            every { workManager.enqueueUniqueWork(any(), any(), capture(oneTimeRequestSlot)) } returns mockk()

            runBlockingTest {
                subject.scheduleMessageSendingWorker(SENDER_ID, MESSAGE_ID)
            }

            oneTimeRequestSlot.captured shouldBeInstanceOf OneTimeWorkRequest::class
            oneTimeRequestSlot.captured.workSpec.input shouldBeEqualTo AndroidSendMessageWorker.workParameters(SENDER_ID, MESSAGE_ID)
            oneTimeRequestSlot.captured.workSpec.constraints.requiredNetworkType shouldBeEqualTo NetworkType.CONNECTED
        }
    }

    @Test
    fun scheduleMessageSendingWorker_MessageIdAndSenderId_KeepExistingWorkPolicyShouldBeUsed() {
        mockkStatic(WorkManager::class) {
            val workManager: WorkManager = mockk()
            every { WorkManager.getInstance(any()) } returns workManager
            every { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk()

            runBlockingTest {
                subject.scheduleMessageSendingWorker(SENDER_ID, MESSAGE_ID)
            }

            verify(exactly = 1) { workManager.enqueueUniqueWork(any(), ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>()) }
        }
    }

    @Test
    fun scheduleMessageSendingWorker_MessageIdAndSenderId_AnUniqueIdBasedOnSenderAndMessageIsCreated() {
        mockkStatic(WorkManager::class) {
            val workManager: WorkManager = mockk()
            every { WorkManager.getInstance(any()) } returns workManager
            every { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk()

            runBlockingTest {
                subject.scheduleMessageSendingWorker(SENDER_ID, MESSAGE_ID)
            }

            verify(exactly = 1) {
                workManager.enqueueUniqueWork("$SENDER_ID-$MESSAGE_ID", any(), any<OneTimeWorkRequest>())
            }
        }
    }

    companion object {
        private const val SENDER_ID = "987"
        private const val MESSAGE_ID = "123"
    }

}
