/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WelcomeCarouselTest {
    @Test fun `carousel jumps from trailing copy to first real page`() {
        assertTrue(shouldJumpToStart(previousPage = 5, currentPage = 6, lastPage = 6, initialPage = 1))
        assertFalse(shouldJumpToStart(previousPage = 0, currentPage = 6, lastPage = 6, initialPage = 1))
    }

    @Test fun `carousel jumps from leading copy to last real page`() {
        assertTrue(shouldJumpToEnd(previousPage = 1, currentPage = 0, lastPage = 6))
        assertFalse(shouldJumpToEnd(previousPage = 6, currentPage = 0, lastPage = 6))
    }
}
