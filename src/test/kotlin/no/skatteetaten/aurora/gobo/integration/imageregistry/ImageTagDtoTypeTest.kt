package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assert
import assertk.assertions.isEqualTo

import org.junit.jupiter.api.Test

class ImageTagDtoTypeTest {

    @Test
    fun `verify tag types are derived correctly`() {

        listOf(
            Pair("SNAPSHOT-feature-AOS-2287-20180102.092832-15-b1.5.5-flange-8.152.18",
                ImageTagType.AURORA_SNAPSHOT_VERSION
            ),
            Pair("feature-AOS-2287-SNAPSHOT", ImageTagType.SNAPSHOT),
            Pair("4.2.4", ImageTagType.BUGFIX),
            Pair("4.2", ImageTagType.MINOR),
            Pair("4", ImageTagType.MAJOR),
            Pair("latest", ImageTagType.LATEST),

            Pair("4.1.3-b1.6.0-flange-8.152.18", ImageTagType.AURORA_VERSION),
            Pair("4b071d3", ImageTagType.COMMIT_HASH),
            Pair("4b07103", ImageTagType.COMMIT_HASH),
            Pair("4007103", ImageTagType.COMMIT_HASH),
            // This isn't ideal but the fallback type is AURORA_VERSION, so even tags that are stricktly not
            // AuroraVersions will get the AURORA_VERSION type
            Pair("weirdness", ImageTagType.AURORA_VERSION)

        ).forEach {
            assert(ImageTagType.typeOf(it.tagString)).isEqualTo(it.expectedType)
        }
    }

    val Pair<String, ImageTagType>.tagString: String get() = this.first
    val Pair<String, ImageTagType>.expectedType: ImageTagType get() = this.second
}