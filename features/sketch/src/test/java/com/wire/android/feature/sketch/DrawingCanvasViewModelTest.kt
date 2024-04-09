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

    private class Arrangement {


        fun arrange() = this to DrawingCanvasViewModel()
    }

    private companion object {
        val DUMMY_OFFSET = Offset(0f, 0f)
    }


}
