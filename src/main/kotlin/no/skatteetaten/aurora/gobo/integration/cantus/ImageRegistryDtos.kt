package no.skatteetaten.aurora.gobo.integration.cantus

import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepoDto
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
                tag.lowercase() == "latest" -> LATEST
                tag.lowercase().endsWith("-snapshot") -> SNAPSHOT
                tag.lowercase().startsWith("snapshot-") -> AURORA_SNAPSHOT_VERSION
                // It is important that COMMIT_HASH is processed before MAJOR to avoid a hash like 1984012 to be
                // considered a MAJOR version (although, technically it could be major version it is not very likely).
                tag.matches(Regex("^[0-9abcdef]{7}$")) -> COMMIT_HASH
                tag.matches(Regex("^\\d+$")) -> MAJOR
                tag.matches(Regex("^\\d+\\.\\d+$")) -> MINOR
                tag.matches(Regex("^\\d+\\.\\d+\\.\\d+$")) -> BUGFIX
                else -> AURORA_VERSION
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
