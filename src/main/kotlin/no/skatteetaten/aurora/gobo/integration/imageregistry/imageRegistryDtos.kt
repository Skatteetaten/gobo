package no.skatteetaten.aurora.gobo.integration.imageregistry

import java.time.Instant

class ImageRegistryServiceErrorException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class ImageRepoDto(val registry: String, val namespace: String, val name: String) {
    val repository: String
        get() = listOf(registry, namespace, name).joinToString("/")
}

enum class ImageTagType {
    LATEST,
    SNAPSHOT,
    MAJOR,
    MINOR,
    BUGFIX,
    AURORA_VERSION,
    AURORA_SNAPSHOT_VERSION;

    companion object {
        fun typeOf(tag: String): ImageTagType {
            return if (tag.toLowerCase().equals("latest")) ImageTagType.LATEST
            else if (tag.toLowerCase().endsWith("-snapshot")) ImageTagType.SNAPSHOT
            else if (tag.toLowerCase().startsWith("snapshot-")) ImageTagType.AURORA_SNAPSHOT_VERSION
            else if (tag.matches(Regex("^\\d+$"))) ImageTagType.MAJOR
            else if (tag.matches(Regex("^\\d+.\\d+$"))) ImageTagType.MINOR
            else if (tag.matches(Regex("^\\d+.\\d+.\\d+$"))) ImageTagType.BUGFIX
            else ImageTagType.AURORA_VERSION
        }
    }
}

data class ImageTagDto(val name: String, var created: Instant) {
    val type: ImageTagType
        get() = ImageTagType.typeOf(name)
}
