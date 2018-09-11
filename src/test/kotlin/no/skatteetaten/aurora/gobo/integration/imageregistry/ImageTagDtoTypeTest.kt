package no.skatteetaten.aurora.gobo.integration.imageregistry

import assertk.assert
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType.AURORA_SNAPSHOT_VERSION
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType.AURORA_VERSION
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType.BUGFIX
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType.LATEST
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType.MAJOR
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType.MINOR
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType.SNAPSHOT
import org.junit.jupiter.api.Test

class ImageTagDtoTypeTest {

    @Test
    fun `verify tag types are derived correctly`() {

        listOf(
            Pair("SNAPSHOT-feature-AOS-2287-20180102.092832-15-b1.5.5-flange-8.152.18", AURORA_SNAPSHOT_VERSION),
            Pair("feature-AOS-2287-SNAPSHOT", SNAPSHOT),
            Pair("4.2.4", BUGFIX),
            Pair("4.2", MINOR),
            Pair("4", MAJOR),
            Pair("latest", LATEST),

            Pair("4.1.3-b1.6.0-flange-8.152.18", AURORA_VERSION),
            // This isn't ideal but the fallback type is AURORA_VERSION, so even tags that are stricktly not
            // AuroraVersions will get the AURORA_VERSION type
            Pair("weirdness", AURORA_VERSION)

        ).forEach {
            assert(ImageTagType.typeOf(it.tagString)).isEqualTo(it.expectedType)
        }
    }

    val Pair<String, ImageTagType>.tagString: String get() = this.first
    val Pair<String, ImageTagType>.expectedType: ImageTagType get() = this.second
}