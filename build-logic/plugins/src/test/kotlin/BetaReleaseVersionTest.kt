import com.wire.android.gradle.version.BetaReleaseVersion
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BetaReleaseVersionTest {
    @Test
    fun `ordinary builds have no beta override`() {
        assertNull(BetaReleaseVersion.resolve("4.36.0", null))
        assertNull(BetaReleaseVersion.resolve("4.36.0", ""))
    }

    @Test
    fun `beta build uses its full tag version`() {
        assertEquals("4.36.0-beta.12", BetaReleaseVersion.resolve("4.36.0", "4.36.0-beta.12"))
        assertEquals("4.36.0-beta.12", BetaReleaseVersion.resolve("4.36.0", "v4.36.0-beta.12"))
    }

    @Test
    fun `beta override must match the base version and a positive unpadded number`() {
        listOf("4.35.0-beta.1", "4.36.0-beta.01", "4.36.0-beta.0", "4.36.0", "4.36.0-rc.1").forEach { version ->
            assertFailsWith<IllegalArgumentException> { BetaReleaseVersion.resolve("4.36.0", version) }
        }
    }
}
