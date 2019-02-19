package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRepoDto
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType.Companion.typeOf
import no.skatteetaten.aurora.gobo.resolvers.GoboConnection
import no.skatteetaten.aurora.gobo.resolvers.GoboEdge
import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo
import no.skatteetaten.aurora.gobo.resolvers.GoboPagedEdges
import org.slf4j.LoggerFactory

data class ImageRepository(
    val registryUrl: String,
    val namespace: String,
    val name: String
) {
    val repository: String
        get() = listOf(registryUrl, namespace, name).joinToString("/")

    companion object {

        private val logger = LoggerFactory.getLogger(ImageRepository::class.java)
        /**
         * @param absoluteImageRepoPath Example docker-registry.aurora.sits.no:5000/no_skatteetaten_aurora/dbh
         */
        fun fromRepoString(absoluteImageRepoPath: String): ImageRepository {
            val (registryUrl, namespace, name) = decompose(absoluteImageRepoPath)
            return ImageRepository(registryUrl, namespace, name)
        }

        private fun decompose(imageRepoString: String): List<String> {
            logger.debug("decomposing segments from repoString=$imageRepoString")
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
    val type: ImageTagType get() = typeOf(name)

    companion object {
        private val logger = LoggerFactory.getLogger(ImageTag::class.java)

        fun fromTagString(tagString: String): ImageTag {

            logger.debug("Create image tag from string=$tagString")
            val repo = tagString.substringBeforeLast(":")
            val tag = tagString.substringAfterLast(":")
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

fun ImageRepository.toImageRepo() = ImageRepoDto(
    registry = this.registryUrl,
    namespace = this.namespace,
    name = this.name
)
