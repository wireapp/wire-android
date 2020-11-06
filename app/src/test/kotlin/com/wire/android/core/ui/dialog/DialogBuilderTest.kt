package com.wire.android.core.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wire.android.R
import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DialogBuilderTest : UnitTest() {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var materialDialogBuilderProvider: MaterialDialogBuilderProvider

    @MockK
    private lateinit var materialAlertDialogBuilder: MaterialAlertDialogBuilder

    @MockK
    private lateinit var alertDialog: AlertDialog

    private lateinit var dialogBuilder: DialogBuilder

    @Before
    fun setUp() {
        every { materialDialogBuilderProvider.provide(context) } returns materialAlertDialogBuilder
        every { materialAlertDialogBuilder.create() } returns alertDialog

        dialogBuilder = DialogBuilder(materialDialogBuilderProvider)
    }

    @Test
    fun `given a block, when showDialog is called, then invokes the block and displays the dialog`() {
        val builderBlock: MaterialAlertDialogBuilder.() -> Unit = mockk(relaxed = true)

        dialogBuilder.showDialog(context, builderBlock)

        verify(exactly = 1) { materialDialogBuilderProvider.provide(context) }
        verify(exactly = 1) { materialAlertDialogBuilder.builderBlock() }
        verify(exactly = 1) { materialAlertDialogBuilder.create() }
        verify(exactly = 1) { alertDialog.show() }
    }

    @Test
    fun `given an errorMessage with title, when showErrorDialog is called, then sets the dialog up with errorMessage and displays it`() {
        val errorMessage = ErrorMessage(title = R.string.app_name, message = R.string.welcome_text)

        dialogBuilder.showErrorDialog(context, errorMessage)

        verify(exactly = 1) { materialAlertDialogBuilder.setTitle(errorMessage.title!!) }
        verify(exactly = 1) { materialAlertDialogBuilder.setMessage(errorMessage.message) }
        verify(exactly = 1) { materialAlertDialogBuilder.setPositiveButton(R.string.ok, any()) }
        verify(exactly = 1) { materialAlertDialogBuilder.create() }
        verify(exactly = 1) { alertDialog.show() }
    }

    @Test
    fun `given an errorMessage without title, when showErrorDialog is called, then sets the dialog up with errorMessage and displays it`() {
        val errorMessage = ErrorMessage(message = R.string.welcome_text)

        dialogBuilder.showErrorDialog(context, errorMessage)

        verify(inverse = true) { materialAlertDialogBuilder.setTitle(any<Int>()) }
        verify(exactly = 1) { materialAlertDialogBuilder.setMessage(errorMessage.message) }
        verify(exactly = 1) { materialAlertDialogBuilder.setPositiveButton(R.string.ok, any()) }
        verify(exactly = 1) { materialAlertDialogBuilder.create() }
        verify(exactly = 1) { alertDialog.show() }
    }
}
