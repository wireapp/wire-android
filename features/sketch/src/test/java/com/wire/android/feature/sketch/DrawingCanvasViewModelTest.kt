package com.wire.android.feature.sketch

import androidx.compose.ui.geometry.Offset
import com.wire.android.feature.sketch.model.DrawingMotionEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingCanvasViewModelTest {

    @Test
    fun givenOnStartDrawingIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventDown() {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onStartDrawing(DUMMY_OFFSET)

        // then
        assertEquals(DrawingMotionEvent.Down, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenOnDrawIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventMove() {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onDraw(DUMMY_OFFSET)

        // then
        assertEquals(DrawingMotionEvent.Move, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenOnStopDrawingIsCalled_WhenCallingTheAction_ThenUpdateStateWithEventUp() {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when
        viewModel.onStopDrawing()

        // then
        assertEquals(DrawingMotionEvent.Up, viewModel.state.drawingMotionEvent)
    }

    @Test
    fun givenStartDrawingEvent_WhenCallingTheAction_ThenUpdateTheStateWithTheInitialPathPosition() {
        // given
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        // when
        drawStrokes(viewModel)

        // then
        with(viewModel.state) {
            assertEquals(DrawingMotionEvent.Down, drawingMotionEvent)
            assertEquals(currentPath.path, paths.first().path)
            assertEquals(currentPosition, MOVED_OFFSET)
        }
    }

    // simulates the drawing of strokes
    private fun drawStrokes(viewModel: DrawingCanvasViewModel) = with(viewModel) {
        onStartDrawing(MOVED_OFFSET)
        onStartDrawingEvent()
    }

    private class Arrangement {
        val viewModel = DrawingCanvasViewModel()

        fun withCurrentPath(newPosition: Offset) = apply {
            viewModel.setNewState(viewModel.state.copy(currentPosition = newPosition))
        }

        fun arrange() = this to viewModel
    }

    private companion object {
        val DUMMY_OFFSET = Offset(0f, 0f)
        val MOVED_OFFSET = Offset(10f, 10f)
    }
}
