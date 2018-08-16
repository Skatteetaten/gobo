package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.PagedEdges
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRepo

data class ImageRepository(
    val registryUrl: String,
    val namespace: String,
    val name: String
) {
    val repository: String
        get() = listOf(registryUrl, namespace, name).joinToString("/")

    companion object {
        /**
         * @param absoluteImageRepoPath Example docker-registry.aurora.sits.no:5000/no_skatteetaten_aurora/dbh
         */
        fun fromRepoString(absoluteImageRepoPath: String): ImageRepository {
            val (registryUrl, namespace, name) = decompose(absoluteImageRepoPath)
            return ImageRepository(registryUrl, namespace, name)
        }

        private fun decompose(imageRepoString: String): List<String> {
            val segments = imageRepoString.split("/")
            if (segments.size != 3) throw IllegalArgumentException("The string [$imageRepoString] does not appear to be a valid image repository reference")
            return segments
        }
    }
}

data class ImageTag(
    val imageRepository: ImageRepository,
    val name: String
) {
    val type: ImageTagType
        get() {
            return if (name.toLowerCase().equals("latest")) ImageTagType.LATEST
            else if (name.toLowerCase().endsWith("-snapshot")) ImageTagType.SNAPSHOT
            else if (name.matches(Regex("^\\d+$"))) ImageTagType.MAJOR
            else if (name.matches(Regex("^\\d+.\\d+$"))) ImageTagType.MINOR
            else if (name.matches(Regex("^\\d+.\\d+.\\d+$"))) ImageTagType.BUGFIX
            else ImageTagType.AURORA_VERSION
        }
}

data class ImageTagEdge(private val node: ImageTag) : DefaultEdge<ImageTag>(node, Cursor(node.name))

data class ImageTagsConnection(
    override val edges: List<ImageTagEdge>,
    override val pageInfo: PageInfo?,
    override val totalCount: Int = edges.size
) : Connection<ImageTagEdge>() {
    constructor(paged: PagedEdges<ImageTagEdge>) : this(paged.edges, paged.pageInfo, paged.totalCount)
}

enum class ImageTagType {
    LATEST,
    SNAPSHOT,
    MAJOR,
    MINOR,
    BUGFIX,
    AURORA_VERSION
}

fun toImageRepo(it: ImageRepository) = ImageRepo(it.registryUrl, it.namespace, it.name)
