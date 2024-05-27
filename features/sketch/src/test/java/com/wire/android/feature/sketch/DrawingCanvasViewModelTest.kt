package com.wire.android.feature.sketch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.wire.android.feature.sketch.model.DrawingMotionEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingCanvasViewModelTest {

    @Test
    fun givenOnStartDrawingIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventDown() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onStartDrawing(INITIAL_OFFSET)

        // then
        assertEquals(DrawingMotionEvent.Down, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenOnDrawIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventMove() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onDraw(INITIAL_OFFSET)

        // then
        assertEquals(DrawingMotionEvent.Move, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenOnStopDrawingIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventUp() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onStopDrawing()

        // then
        assertEquals(DrawingMotionEvent.Up, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenStartDrawingEvent_WhenCallingTheAction_ThenUpdateTheStateWithTheInitialPathPosition() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()
        assertEquals(viewModel.state.currentPosition, Offset.Unspecified)

        // when
        startDrawing(viewModel)

        // then
        with(viewModel.state) {
            assertEquals(DrawingMotionEvent.Down, drawingMotionEvent)
            assertEquals(currentPath.path, paths.first().path)
            assertEquals(currentPosition, INITIAL_OFFSET)
        }
    }

    @Test
    fun givenDrawingEvent_WhenCallingTheAction_ThenUpdateTheStateWithTheCurrentMovingPathPosition() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()
        assertEquals(viewModel.state.currentPosition, Offset.Unspecified)

        // when
        draw(viewModel, MOVED_OFFSET)

        // then
        with(viewModel.state) {
            assertEquals(DrawingMotionEvent.Move, drawingMotionEvent)
            assertEquals(currentPath.path, paths.first().path)
            assertEquals(currentPosition, MOVED_OFFSET)
        }
    }

    @Test
    fun givenDrawingEventPersisted_WhenCallingTheUndoAction_ThenUpdateShouldNotHaveDuplicatedPathAndRemoveLast() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()
        assertEquals(viewModel.state.currentPosition, Offset.Unspecified)

        // when - then
        draw(viewModel, MOVED_OFFSET)
        assertEquals(1, viewModel.state.paths.size)
        assertEquals(0, viewModel.state.pathsUndone.size)

        // repeated path
        draw(viewModel, MOVED_OFFSET)
        assertEquals(2, viewModel.state.paths.size)
        assertEquals(0, viewModel.state.pathsUndone.size)

        // then
        viewModel.onUndoLastStroke()
        assertEquals(0, viewModel.state.paths.size)
        assertEquals(1, viewModel.state.pathsUndone.size)
    }

    @Test
    fun givenStopDrawingEvent_WhenCallingTheAction_ThenUpdateTheStateWithTheFinalPathPosition() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()
        assertEquals(viewModel.state.currentPosition, Offset.Unspecified)

        // when
        stopDrawing(viewModel)

        // then
        with(viewModel.state) {
            assertEquals(DrawingMotionEvent.Idle, drawingMotionEvent)
            assertEquals(currentPosition, Offset.Unspecified)
        }
    }

    @Test
    fun givenOnColorChanged_WhenCallingTheAction_ThenUpdateCurrentPathWithTheSelectedColor() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()
        assertEquals(viewModel.state.currentPath.color, Color.Black)

        // when
        val newColor = Color.Magenta
        viewModel.onColorChanged(newColor)

        // then
        with(viewModel.state) {
            assertEquals(currentPath.color, newColor)
        }
    }

    @Test
    fun givenWeWantToDiscard_WhenCallingTheAction_ThenUpdateStateToShowConfirmation() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onShowConfirmationDialog()

        // then
        with(viewModel.state) {
            assertEquals(true, showConfirmationDialog)
        }
    }

    @Test
    fun givenWeCancelToDiscard_WhenCallingTheAction_ThenUpdateStateToHideConfirmation() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onHideConfirmationDialog()

        // then
        with(viewModel.state) {
            assertEquals(false, showConfirmationDialog)
        }
    }

    private fun stopDrawing(viewModel: DrawingCanvasViewModel, movedOffset: Offset = MOVED_OFFSET) = with(viewModel) {
        draw(viewModel, movedOffset)
        onStopDrawing()
        onStopDrawingEvent()
    }

    // simulates the drawing of strokes
    private fun draw(viewModel: DrawingCanvasViewModel, movedOffset: Offset = MOVED_OFFSET) = with(viewModel) {
        startDrawing(viewModel)
        onDraw(movedOffset)
        onDrawEvent()
    }

    // simulates the start of drawing of strokes
    private fun startDrawing(viewModel: DrawingCanvasViewModel) = with(viewModel) {
        onStartDrawing(INITIAL_OFFSET)
        onStartDrawingEvent()
    }

    private class Arrangement {
        val viewModel = DrawingCanvasViewModel()
        fun arrange() = this to viewModel
    }

    private companion object {
        val INITIAL_OFFSET = Offset(0f, 0f)
        val MOVED_OFFSET = Offset(10f, 10f)
    }
}
