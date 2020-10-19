package no.skatteetaten.aurora.gobo.graphql.imagerepository

import com.expediagroup.graphql.annotations.GraphQLIgnore
import graphql.schema.DataFetchingEnvironment
import java.time.Instant
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType.Companion.typeOf
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.GoboPageInfo
import no.skatteetaten.aurora.gobo.graphql.GoboPagedEdges
import no.skatteetaten.aurora.gobo.graphql.load
import no.skatteetaten.aurora.gobo.graphql.loadMultipleKeys
import no.skatteetaten.aurora.gobo.graphql.pageEdges

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

    // TODO fix schema? isFullyQualified cannot be null
    val isFullyQualified: Boolean? get() = registryUrl != null

    val repository: String
        get() = listOf(registryUrl, namespace, name).joinToString("/")

    fun toImageRepo(filter: String? = null) = ImageRepoDto(
        registry = this.registryUrl,
        namespace = this.namespace,
        name = this.name,
        filter = filter
    )

    // TODO should this be named tags? it returns a list
    suspend fun tag(
        names: List<String>,
        dfe: DataFetchingEnvironment
    ): List<ImageWithType?> {

        // TODO fix schema? isFullyQualified cannot be null
        if (!isFullyQualified!!) {
            return emptyList()
        }

        val imageTags = names.map { ImageTag(this, it) }
        val values = dfe.loadMultipleKeys<ImageTag, Image>(imageTags)
        return values.map {
            ImageWithType(it.key.name, it.value.get())
        }
    }

    suspend fun tags(
        types: List<ImageTagType>?,
        filter: String?,
        first: Int,
        after: String?,
        dfe: DataFetchingEnvironment
    ): ImageTagsConnection {
        // TODO fix schema? isFullyQualified cannot be null
        val tagsDto = if (!isFullyQualified!!) {
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

    suspend fun guiUrl(dfe: DataFetchingEnvironment): String? {
        val guiUrl: GuiUrl = dfe.load(this)
        return guiUrl.url
    }

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
