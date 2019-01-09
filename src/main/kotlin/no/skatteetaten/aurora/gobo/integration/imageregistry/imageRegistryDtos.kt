package no.skatteetaten.aurora.gobo.integration.imageregistry

import java.time.Instant

data class ImageRepoDto(val registry: String, val namespace: String, val name: String) {
    val repository: String
        get() = listOf(registry, namespace, name).joinToString("/")

    val imageName: String
        get() = listOf(namespace, name).joinToString("/")
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
                // considered a MAJOR version (although, technically it coulld be major version it is not very likely).
                tag.matches(Regex("^[0-9abcdef]{7}$")) -> ImageTagType.COMMIT_HASH
                tag.matches(Regex("^\\d+$")) -> ImageTagType.MAJOR
                tag.matches(Regex("^\\d+\\.\\d+$")) -> ImageTagType.MINOR
                tag.matches(Regex("^\\d+\\.\\d+\\.\\d+$")) -> ImageTagType.BUGFIX
                else -> ImageTagType.AURORA_VERSION
            }
        }
    }
}

data class ImageTagDto(val name: String, var created: Instant) {
    val type: ImageTagType
        get() = ImageTagType.typeOf(name)
}
