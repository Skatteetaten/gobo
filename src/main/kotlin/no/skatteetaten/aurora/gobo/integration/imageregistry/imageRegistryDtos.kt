package no.skatteetaten.aurora.gobo.integration.imageregistry

import java.time.Instant

data class ImageRepoDto(
    val registry: String,
    val namespace: String,
    val name: String,
    val tag: String = ""
) {
    val repository: String
        get() = listOf(registry, namespace, name).joinToString("/")

    val imageName: String
        get() {
            return if (tag.isEmpty()) "$namespace/$name"
            else "$namespace/$name/$tag"
        }

    val mappedTemplateVars = mapOf(
        "namespace" to namespace,
        "name" to name,
        "tag" to tag
    )
}

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
                tag.toLowerCase() == "latest" -> ImageTagType.LATEST
                tag.toLowerCase().endsWith("-snapshot") -> ImageTagType.SNAPSHOT
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

data class Tag(val name: String, val type: ImageTagType)

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
    val dockerDigest: String,
    val name: String,
    val created: Instant?
) {
    companion object {
        fun toDto(imageTagResponse: AuroraResponse<ImageTagResource>, tagName: String): ImageTagDto =
            ImageTagDto(
                dockerDigest = imageTagResponse.items[0].dockerDigest,
                name = tagName,
                created = imageTagResponse.items[0].timeline.buildEnded
            )
    }
}
