package no.skatteetaten.aurora.gobo.integration.cantus

import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepoDto
import java.time.Instant

enum class ImageTagType {
    LATEST,
    SNAPSHOT,
    MAJOR,
    MINOR,
    BUGFIX,
    AURORA_VERSION,
    AURORA_SNAPSHOT_VERSION,
    COMMIT_HASH;

    companion object {
        fun typeOf(tag: String): ImageTagType {
            return when {
                tag.toLowerCase() == "latest" -> LATEST
                tag.toLowerCase().endsWith("-snapshot") -> SNAPSHOT
                tag.toLowerCase().startsWith("snapshot-") -> ImageTagType.AURORA_SNAPSHOT_VERSION
                // It is important that COMMIT_HASH is processed before MAJOR to avoid a hash like 1984012 to be
                // considered a MAJOR version (although, technically it could be major version it is not very likely).
                tag.matches(Regex("^[0-9abcdef]{7}$")) -> ImageTagType.COMMIT_HASH
                tag.matches(Regex("^\\d+$")) -> ImageTagType.MAJOR
                tag.matches(Regex("^\\d+\\.\\d+$")) -> ImageTagType.MINOR
                tag.matches(Regex("^\\d+\\.\\d+\\.\\d+$")) -> ImageTagType.BUGFIX
                else -> ImageTagType.AURORA_VERSION
            }
        }
    }
}

data class Tag(val name: String, val type: ImageTagType = ImageTagType.typeOf(name))

data class TagsDto(val tags: List<Tag>) {
    companion object {
        fun toDto(tagResponse: AuroraResponse<TagResource>) =
            TagsDto(
                tagResponse.items.map {
                    Tag(
                        name = it.name,
                        type = it.type
                    )
                }.reversed()
            )
    }
}

data class ImageTagDto(
    val dockerDigest: String? = null,
    val imageTag: String,
    val created: Instant? = null,
    val imageRepoDto: ImageRepoDto
) {
    companion object {
        fun toDto(
            imageTagResponse: AuroraResponse<ImageTagResource>,
            tagName: String,
            imageRepoDto: ImageRepoDto
        ): ImageTagDto =
            ImageTagDto(
                dockerDigest = imageTagResponse.items[0].dockerDigest,
                imageTag = tagName,
                created = imageTagResponse.items[0].timeline.buildEnded,
                imageRepoDto = imageRepoDto
            )
    }
}
