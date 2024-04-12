package com.wire.android.feature.sketch

import androidx.compose.ui.geometry.Offset
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
        draw(viewModel)

        // then
        with(viewModel.state) {
            assertEquals(DrawingMotionEvent.Move, drawingMotionEvent)
            assertEquals(currentPath.path, paths.first().path)
            assertEquals(currentPosition, MOVED_OFFSET)
        }
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

    private fun stopDrawing(viewModel: DrawingCanvasViewModel) = with(viewModel) {
        draw(viewModel)
        onStopDrawing()
        onStopDrawingEvent()
    }

    // simulates the drawing of strokes
    private fun draw(viewModel: DrawingCanvasViewModel) = with(viewModel) {
        startDrawing(viewModel)
        onDraw(MOVED_OFFSET)
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
