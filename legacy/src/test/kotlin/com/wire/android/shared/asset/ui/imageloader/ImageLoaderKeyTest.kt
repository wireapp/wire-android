package com.wire.android.shared.asset.ui.imageloader

import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.wire.android.AndroidTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Test

@Suppress("ReplaceCallWithBinaryOperator")
class ImageLoaderKeyTest : AndroidTest() {

    @Test
    fun `given two imageLoaderKeys, when both have the same parameters, then their toString, hashCode & equals methods behave the same`() {
        val options = Options()
        val key1 = ImageLoaderKey("unique_id", 13, 29, options)
        val key2 = ImageLoaderKey("unique_id", 13, 29, options)

        key1.toString() shouldBeEqualTo key2.toString()
        key1.hashCode() shouldBeEqualTo key2.hashCode()
        key1.equals(key2) shouldBeEqualTo true
    }

    @Test
    fun `given two imageLoaderKeys, when they have different uniqueIds, then their toString, hashCode & equals methods should differ`() {
        val options = Options()
        val key1 = ImageLoaderKey("unique_id", 13, 29, options)
        val key2 = ImageLoaderKey("another_unique_id", 13, 29, options)

        key1.toString() shouldNotBeEqualTo key2.toString()
        key1.hashCode() shouldNotBeEqualTo key2.hashCode()
        key1.equals(key2) shouldNotBeEqualTo true
    }

    @Test
    fun `given two imageLoaderKeys, when they have different widths, then their toString, hashCode & equals methods should differ`() {
        val options = Options()
        val key1 = ImageLoaderKey("unique_id", 13, 29, options)
        val key2 = ImageLoaderKey("unique_id", 987, 29, options)

        key1.toString() shouldNotBeEqualTo key2.toString()
        key1.hashCode() shouldNotBeEqualTo key2.hashCode()
        key1.equals(key2) shouldNotBeEqualTo true
    }

    @Test
    fun `given two imageLoaderKeys, when they have different heights, then their toString, hashCode & equals methods should differ`() {
        val options = Options()
        val key1 = ImageLoaderKey("unique_id", 13, 29, options)
        val key2 = ImageLoaderKey("unique_id", 13, 2438, options)

        key1.toString() shouldNotBeEqualTo key2.toString()
        key1.hashCode() shouldNotBeEqualTo key2.hashCode()
        key1.equals(key2) shouldNotBeEqualTo true
    }

    @Test
    fun `given two imageLoaderKeys, when they have different options, then their toString, hashCode & equals methods should differ`() {
        val optionsWithMemory = Options().also { it.set(Option.memory("memory"), 120) }
        val key1 = ImageLoaderKey("unique_id", 13, 29, optionsWithMemory)
        val key2 = ImageLoaderKey("unique_id", 13, 29, Options())

        key1.toString() shouldNotBeEqualTo key2.toString()
        key1.hashCode() shouldNotBeEqualTo key2.hashCode()
        key1.equals(key2) shouldNotBeEqualTo true
    }
}
