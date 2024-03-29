package no.skatteetaten.aurora.gobo.graphql.imagerepository

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.loadValue
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType.Companion.typeOf
import java.time.Instant
import java.util.concurrent.CompletableFuture

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
    fun tag(
        names: List<String>,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<List<ImageWithType?>> {

        if (!isFullyQualified()) {
            return CompletableFuture.completedFuture(emptyList())
        }

        val imageTags = names.map { ImageTag(this, it) }
        return dfe.loadValue(keys = imageTags, loaderClass = MultipleImagesDataLoader::class)
    }

    fun versions(dfe: DataFetchingEnvironment) =
        dfe.loadValue<ImageRepository, DataFetcherResult<List<ImageTag>?>>(this, loaderClass = VersionsDataLoader::class)

    fun guiUrl(dfe: DataFetchingEnvironment) =
        dfe.loadValue<ImageRepository, String?>(key = this, loaderClass = GuiUrlDataLoader::class)

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

    fun image(dfe: DataFetchingEnvironment) = dfe.loadValue<ImageTag, DataFetcherResult<Image?>>(key = this, loaderClass = ImageDataLoader::class)

    fun version(dfe: DataFetchingEnvironment) = dfe.loadValue<ImageTag, Image?>(key = this, loaderClass = VersionDataLoader::class)

    companion object {
        fun fromTagString(tagString: String, lastDelimiter: String = ":"): ImageTag {
            logger.debug("Create image tag from string=$tagString")
            val repo = tagString.substringBeforeLast(lastDelimiter)
            val tag = tagString.substringAfterLast(lastDelimiter)
            return ImageTag(imageRepository = ImageRepository.fromRepoString(repo), name = tag)
        }
    }
}

data class Image(val buildTime: Instant?, val imageReference: String)
