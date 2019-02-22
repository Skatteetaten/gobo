package no.skatteetaten.aurora.gobo.integration.imageregistry

import uk.q3c.rest.hal.HalResource
import java.time.Instant

data class TagResource(val name: String, val type: ImageTagType = ImageTagType.typeOf(name)) : HalResource()

data class ImageTagResource(
    val auroraVersion: String? = null,
    val appVersion: String? = null,
    val timeline: ImageBuildTimeline,
    val dockerVersion: String,
    val dockerDigest: String,
    val java: JavaImage? = null,
    val node: NodeJsImage? = null
) : HalResource()

data class JavaImage(
    val major: String,
    val minor: String,
    val build: String,
    val jolokia: String?
)

data class ImageBuildTimeline(
    val buildStarted: Instant?,
    val buildEnded: Instant?
)

data class NodeJsImage(
    val nodeJsVersion: String
)

data class AuroraResponse<T : HalResource>(
    val items: List<T> = emptyList(),
    val success: Boolean = true,
    val message: String = "OK",
    val exception: Throwable? = null,
    val count: Int = items.size
) : HalResource()
