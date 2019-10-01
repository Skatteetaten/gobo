package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoDto
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType.Companion.typeOf
import no.skatteetaten.aurora.gobo.integration.cantus.decomposeToImageRepoSegments
import no.skatteetaten.aurora.gobo.resolvers.GoboConnection
import no.skatteetaten.aurora.gobo.resolvers.GoboEdge
import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo
import no.skatteetaten.aurora.gobo.resolvers.GoboPagedEdges
import java.time.Instant

private val logger = KotlinLogging.logger {}

data class ImageRepository(
    val registryUrl: String,
    val namespace: String,
    val name: String
) {
    val repository: String
        get() = listOf(registryUrl, namespace, name).joinToString("/")

    fun toImageRepo(filter: String? = null) = ImageRepoDto(
        registry = this.registryUrl,
        namespace = this.namespace,
        name = this.name,
        filter = filter
    )

    companion object {
        /**
         * @param absoluteImageRepoPath Example docker-registry.aurora.sits.no:5000/no_skatteetaten_aurora/dbh
         */
        fun fromRepoString(absoluteImageRepoPath: String): ImageRepository {
            val (registryUrl, namespace, name) = absoluteImageRepoPath.decomposeToImageRepoSegments()
            return ImageRepository(registryUrl, namespace, name)
        }
    }
}

data class ImageTag(
    val imageRepository: ImageRepository,
    val name: String
) {
    val type: ImageTagType get() = typeOf(name)

    companion object {
        fun fromTagString(tagString: String, lastDelimiter: String = ":"): ImageTag {
            logger.debug("Create image tag from string=$tagString")
            val repo = tagString.substringBeforeLast(lastDelimiter)
            val tag = tagString.substringAfterLast(lastDelimiter)
            return ImageTag(imageRepository = ImageRepository.fromRepoString(repo), name = tag)
        }
    }
}

data class ImageTagEdge(val node: ImageTag) : GoboEdge(node.name)

data class ImageTagsConnection(
    override val edges: List<ImageTagEdge>,
    override val pageInfo: GoboPageInfo?,
    override val totalCount: Int = edges.size
) : GoboConnection<ImageTagEdge>() {
    constructor(paged: GoboPagedEdges<ImageTagEdge>) : this(paged.edges, paged.pageInfo, paged.totalCount)
}

data class Image(val buildTime: Instant?, val imageReference: String)