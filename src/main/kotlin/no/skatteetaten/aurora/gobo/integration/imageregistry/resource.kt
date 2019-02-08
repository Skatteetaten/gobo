package no.skatteetaten.aurora.gobo.integration.imageregistry

import uk.q3c.rest.hal.HalResource
import java.time.Instant

data class TagResource(val name: String, val type: ImageTagType = ImageTagType.typeOf(name)) : HalResource()

data class GroupedTagResource(
    val group: String,
    val tagResource: List<TagResource>,
    val itemsInGroup: Int = tagResource.size
) : HalResource()

data class ImageTagResource(
    val auroraVersion: String? = null,
    val appVersion: String? = null,
    val timeline: ImageBuildTimeline,
    val dockerVersion: String,
    val dockerDigest: String,
    val java: JavaImage? = null,
    val node: NodeImage? = null
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

data class NodeImage(
    val nodeVersion: String
)

data class AuroraResponse<T : HalResource>(
    val items: List<T> = emptyList(),
    val success: Boolean = true,
    val message: String = "OK",
    val exception: Throwable? = null,
    val count: Int = items.size
) : HalResource() {

    // TODO: trenger vi denne?
    val item: T?
        get() {
            if (count == 1) {
                return items.first()
            }
            return null
        }
}
/*

@Component
class ImageTagResourceAssembler {
    fun toResource(manifestDto: ImageManifestDto, message: String): AuroraResponse<ImageTagResource> =
        AuroraResponse(
            success = true,
            message = message,
            items = listOf(
                ImageTagResource(
                    java = JavaImage.fromDto(manifestDto),
                    dockerDigest = manifestDto.dockerDigest,
                    dockerVersion = manifestDto.dockerVersion,
                    appVersion = manifestDto.appVersion,
                    auroraVersion = manifestDto.auroraVersion,
                    timeline = ImageBuildTimeline.fromDto(manifestDto),
                    node = NodeImage.fromDto(manifestDto)
                )
            )
        )

    fun toResource(tags: ImageTagsWithTypeDto, message: String): AuroraResponse<TagResource> =
        AuroraResponse(
            success = true,
            message = message,
            items = tags.tags.map {
                TagResource(
                    name = it.name
                )
            }
        )

    fun toGroupedResource(tags: ImageTagsWithTypeDto, message: String): AuroraResponse<GroupedTagResource> =
        AuroraResponse(
            success = true,
            message = message,
            items = tags.tags.groupBy {
                it.type
            }.map { groupedTag ->
                GroupedTagResource(
                    group = groupedTag.key.toString(),
                    tagResource = groupedTag.value.map {
                        TagResource(
                            name = it.name,
                            type = it.type
                        )
                    }
                )
            }
        )
}*/
