package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.catch
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

enum class ImageReferenceTestData(val reference: String, val result: List<String>) {
    LIBRARY("foobar", listOf("library", "foobar", "latest")),
    LATEST("bar/foobar", listOf("bar", "foobar", "latest")),
    FULL("bar/foobar/1", listOf("bar", "foobar", "1"))
}

class DecomposeImageReferenceTest {

    @ParameterizedTest
    @EnumSource(ImageReferenceTestData::class)
    fun `should parse image references`(ref: ImageReferenceTestData) {
        assertThat(ref.reference.decomposeToImageRepoSegments()).isEqualTo(ref.result)
    }

    @Test
    fun `should fail if too many segments`() {

        val exception = catch {
            "foo/bar/baz/jalla".decomposeToImageRepoSegments()
        }
        assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException::class)
    }
}