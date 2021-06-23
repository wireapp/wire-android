import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDateTime

@RunWith(JUnit4::class)
class VersionizerTest {

    @Test
    fun `given version generator when I generate two versions AT THE SAME TIME I should get the same version number`() {
        val dateTime = LocalDateTime.now()

        Versionizer(dateTime).versionCode shouldBeEqualTo Versionizer(dateTime).versionCode
    }


    @Test
    fun `given version generator when I generate a version I should get the same version number on the current Android project`() {
        val dateTime = LocalDateTime.of(2021, 6, 23, 13, 54, 28)

        Versionizer(dateTime).versionCode shouldBeEqualTo 548966
    }

    @Test
    fun `given version generator when I generate a new version AFTER 10 SECONDS then I should get an directly incremented number`() {
        val versionCodeOld = Versionizer(LocalDateTime.now().minusSeconds(10)).versionCode
        val versionCodeNew = Versionizer().versionCode

        versionCodeOld + 1 shouldBeEqualTo versionCodeNew
    }

    @Test
    fun `given version generator when I generate a new version THE NEXT DAY then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusDays(1)).versionCode

        assertVersionCodes(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN ONE YEAR then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusYears(1)).versionCode

        assertVersionCodes(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN ONE and TWO YEARS then I should get an incremented version number`() {
        val now = LocalDateTime.now()
        val oldVersionCode = Versionizer(now.plusYears(1)).versionCode
        val newVersionCode = Versionizer(now.plusYears(2)).versionCode

        assertVersionCodes(oldVersionCode, newVersionCode)
    }

    @Test
    fun `given version generator when I generate a new version IN TEN YEARS then I should get an incremented version number`() {
        val oldVersionCode = Versionizer().versionCode
        val newVersionCode = Versionizer(LocalDateTime.now().plusYears(10)).versionCode

        // This will break 655 years from now.
        assertVersionCodes(oldVersionCode, newVersionCode)
    }

    private fun assertVersionCodes(oldVersionCode: Int, newVersionCode: Int) {
        oldVersionCode shouldBeGreaterThan 0
        newVersionCode shouldBeGreaterThan 0
        oldVersionCode shouldBeLessThan newVersionCode
        oldVersionCode shouldBeLessThan Versionizer.MAX_VERSION_CODE_ALLOWED
    }
}
