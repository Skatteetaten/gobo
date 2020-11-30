package no.skatteetaten.aurora.gobo.integration.cantus

import java.time.Instant
import uk.q3c.rest.hal.HalResource

data class TagResource(val name: String, val type: ImageTagType = ImageTagType.typeOf(name)) : HalResource()

data class ImageTagResource(
    val auroraVersion: String? = null,
    val appVersion: String? = null,
    val timeline: ImageBuildTimeline,
    val dockerVersion: String,
    val dockerDigest: String,
    val java: JavaImage? = null,
    val node: NodeJsImage? = null,
    val requestUrl: String
) : HalResource()

data class NodeJsImage(val nodeJsVersion: String)

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

data class CantusFailure(
    val url: String,
    val errorMessage: String
)

data class AuroraResponse<T : Any>(
    val items: List<T> = emptyList(),
    val failure: List<CantusFailure> = emptyList(),
    val success: Boolean = true,
    val message: String = "OK",
    val failureCount: Int = failure.size,
    val successCount: Int = items.size,
    val count: Int = failureCount + successCount
) : HalResource()
