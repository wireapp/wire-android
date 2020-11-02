package com.wire.android.framework.android

import androidx.core.os.bundleOf
import com.wire.android.AndroidTest
import org.amshove.kluent.shouldBe
import org.junit.Test

class FragmentAssertionsTest : AndroidTest() {

    @Test
    fun `given contentEquals called on a bundle, when two bundles contain the same mappings, then returns true`() {
        val bundle1 = bundleOf("key1" to "value1", "key2" to 4, "key3" to arrayOf("a", "b"), "key4" to bundleOf("x" to 9))
        val bundle2 = bundleOf("key1" to "value1", "key2" to 4, "key3" to arrayOf("a", "b"), "key4" to bundleOf("x" to 9))

        bundle1.contentEquals(bundle2) shouldBe true
        bundle2.contentEquals(bundle1) shouldBe true
    }

    @Test
    fun `given contentEquals called on a bundle, when one of the bundles is a subset of the other, then returns false`() {
        val bundle1 = bundleOf("key1" to "value1", "key2" to 4, "key3" to arrayOf("a", "b"))
        val bundle2 = bundleOf("key1" to "value1", "key2" to 4)

        bundle1.contentEquals(bundle2) shouldBe false
        bundle2.contentEquals(bundle1) shouldBe false
    }

    @Test
    fun `given contentEquals called on a bundle, when two bundles are not equal, then returns false`() {
        val bundle1 = bundleOf("key3" to arrayOf("a", "b"))
        val bundle2 = bundleOf("key1" to "value1", "key2" to 4)

        bundle1.contentEquals(bundle2) shouldBe false
        bundle2.contentEquals(bundle1) shouldBe false
    }
}
