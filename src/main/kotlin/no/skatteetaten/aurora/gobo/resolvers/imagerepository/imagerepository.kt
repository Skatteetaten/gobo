package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.expediagroup.graphql.annotations.GraphQLIgnore
import graphql.schema.DataFetchingEnvironment
import java.time.Instant
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType.Companion.typeOf
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import no.skatteetaten.aurora.gobo.resolvers.*

private val logger = KotlinLogging.logger {}

data class ImageRepoDto(
    val registry: String?,
    val namespace: String,
    val name: String,
    val filter: String? = null
) {
    val repository: String
        get() = listOf(registry, namespace, name).joinToString("/")

    val imageName: String
        get() = "$namespace/$name"

    @GraphQLIgnore
    val mappedTemplateVars = mapOf(
        "namespace" to namespace,
        "imageTag" to name
    )
}

data class ImageRepository(
    val registryUrl: String?,
    val namespace: String,
    val name: String
) {

    val isFullyQualified: Boolean get() = registryUrl != null

    val repository: String
        get() = listOf(registryUrl, namespace, name).joinToString("/")

    fun toImageRepo(filter: String? = null) = ImageRepoDto(
        registry = this.registryUrl,
        namespace = this.namespace,
        name = this.name,
        filter = filter
    )

    suspend fun tag(
            names: List<String>,
            dfe: DataFetchingEnvironment
    ): List<ImageWithType?> {

        if (!isFullyQualified) {
                return emptyList<ImageWithType?>()
        }

        val dataloader = dfe.multipleKeysLoader<ImageTag,Image>()

        val tags = names.map { name ->
            dataloader.load(ImageTag(imageRepository, name)).thenApply {
                it?.let {
                    ImageWithType(name, it)
                }
            }
        }

        return tags.join()
    }


    suspend fun tags(
            types: List<ImageTagType>?,
            filter: String?,
            first: Int?,
            after: String?,
            dfe: DataFetchingEnvironment
    ): ImageTagsConnection {
        val tagsDto = if (!isFullyQualified) {
            TagsDto(emptyList())
        } else {
            dfe.load(toImageRepo(filter))
        }
        val imageTags = tagsDto.tags.toImageTags(this, types)
        val allEdges = imageTags.map { ImageTagEdge(it) }
        return ImageTagsConnection(pageEdges(allEdges, first, after))
    }

    private fun List<Tag>.toImageTags(imageRepository: ImageRepository, types: List<ImageTagType>?) = this
        .map { ImageTag(imageRepository = imageRepository, name = it.name) }
        .filter { types == null || it.type in types }


    companion object {
        /**
         * @param absoluteImageRepoPath Example docker-registry.aurora.sits.no:5000/no_skatteetaten_aurora/dbh
         */
        fun fromRepoString(absoluteImageRepoPath: String): ImageRepository {

            val segments = absoluteImageRepoPath.split("/")
            if (segments.size != 3) {
                val (namespace, name) = segments
                return ImageRepository(null, namespace, name)
            }
            val (registryUrl, namespace, name) = segments
            return ImageRepository(registryUrl, namespace, name)
        }
    }
}

data class ImageWithType(
    val name: String,
    val image: Image
) {
    val type: ImageTagType get() = typeOf(name)
}

data class ImageTag(
    val imageRepository: ImageRepository,
    val name: String
) {
    val type: ImageTagType get() = typeOf(name)

    suspend fun image(dfe: DataFetchingEnvironment): Image? = dfe.load<ImageTag, Image>(this)

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
    val edges: List<ImageTagEdge>,
    val pageInfo: GoboPageInfo?,
    val totalCount: Int = edges.size
) {
    constructor(paged: GoboPagedEdges<ImageTagEdge>) : this(paged.edges, paged.pageInfo, paged.totalCount)
}

data class Image(val buildTime: Instant?, val imageReference: String)
