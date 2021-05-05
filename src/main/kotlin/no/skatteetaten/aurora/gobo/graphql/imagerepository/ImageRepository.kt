package no.skatteetaten.aurora.gobo.graphql.imagerepository

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import graphql.execution.DataFetcherResult
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
import no.skatteetaten.aurora.gobo.graphql.loadOrThrow
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

    val repository: String
        get() = listOf(registryUrl, namespace, name).joinToString("/")

    fun isFullyQualified() = registryUrl != null

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

        if (!isFullyQualified()) {
            return emptyList()
        }

        val imageTags = names.map { ImageTag(this, it) }
        val values = dfe.loadMultipleKeys<ImageTag, Image>(imageTags)
        return values.map {
            if (it.value.hasErrors()) {
                null
            } else {
                ImageWithType(it.key.name, it.value.data)
            }
        }
    }

    suspend fun tags(
        types: List<ImageTagType>? = null,
        filter: String? = null,
        first: Int,
        after: String? = null,
        dfe: DataFetchingEnvironment
    ): DataFetcherResult<ImageTagsConnection> {
        val tagsDto = if (!isFullyQualified()) {
            DataFetcherResult.newResult<TagsDto>().data(TagsDto(emptyList())).build()
        } else {
            dfe.load(toImageRepo(filter))
        }

        val tags = tagsDto.data?.tags ?: emptyList()
        val imageTags = tags.toImageTags(this, types)
        val allEdges = imageTags.map { ImageTagEdge(it) }
        return DataFetcherResult.newResult<ImageTagsConnection>()
            .data(ImageTagsConnection(pageEdges(allEdges, first, after)))
            .errors(tagsDto.errors)
            .build()
    }

    private fun List<Tag>.toImageTags(imageRepository: ImageRepository, types: List<ImageTagType>?) = this
        .map { ImageTag(imageRepository = imageRepository, name = it.name) }
        .filter { types == null || it.type in types }

    suspend fun guiUrl(dfe: DataFetchingEnvironment): String? {
        val guiUrl: GuiUrl = dfe.loadOrThrow(this)
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

    suspend fun image(dfe: DataFetchingEnvironment) = dfe.load<ImageTag, Image?>(this)

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
