package no.skatteetaten.aurora.gobo.resolvers.imagerepository

/*
private val logger = KotlinLogging.logger {}

@Component
class ImageRepositoryQueryResolver : GraphQLQueryResolver {

    fun getImageRepositories(repositories: List<String>, dfe: DataFetchingEnvironment): List<ImageRepository> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot access imagrepositories")
        if (repositories.isEmpty()) throw GoboException("repositories is empty")

        return repositories.map { ImageRepository.fromRepoString(it) }
    }
}

@Component
class ImageRepositoryResolver(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking,
    val aurora: AuroraIntegration
) : GraphQLResolver<ImageRepository> {

    fun guiUrl(
        imageRepository: ImageRepository,
        dfe: DataFetchingEnvironment
    ): String? {
        logger.debug { "Trying to find guiUrl for $imageRepository with configured repositories ${aurora.docker.values.map { it.url }.joinToString { "," }}" }
        val replacer =
            StringSubstitutor(mapOf("group" to imageRepository.namespace, "name" to imageRepository.name), "@", "@")
        return aurora.docker.values.find { it.url == imageRepository.registryUrl }?.let {
            replacer.replace(it.guiUrlPattern)
        }
    }

    fun tag(
        imageRepository: ImageRepository,
        names: List<String>,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<List<ImageWithType?>> {

        if (!imageRepository.isFullyQualified) {
            return CompletableFuture.supplyAsync {
                emptyList<ImageWithType?>()
            }
        }

        val dataloader = dfe.multipleKeysLoader(ImageTagDataLoader::class)

        val tags = names.map { name ->
            dataloader.load(ImageTag(imageRepository, name)).thenApply {
                it?.let {
                    ImageWithType(name, it)
                }
            }
        }

        return tags.join()
    }

    fun <A> List<CompletableFuture<A>>.join(): CompletableFuture<List<A>> {
        return CompletableFuture.allOf(*this.toTypedArray()).thenApply {
            this.map { it.join() }
        }
    }

    fun tags(
        imageRepository: ImageRepository,
        types: List<ImageTagType>?,
        filter: String?,
        first: Int? = null,
        after: String? = null,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<ImageTagsConnection> {

        val tagsDto: CompletableFuture<TagsDto> = if (!imageRepository.isFullyQualified) {
            CompletableFuture.supplyAsync {
                TagsDto(emptyList())
            }
        } else {
            dfe.loader(ImageTagListDataLoader::class).load(imageRepository.toImageRepo(filter))
        }
        return tagsDto.thenApply { dto ->
            val imageTags = dto.tags.toImageTags(imageRepository, types)
            val allEdges = imageTags.map { ImageTagEdge(it) }
            ImageTagsConnection(pageEdges(allEdges, first, after))
        }
    }

    fun List<Tag>.toImageTags(imageRepository: ImageRepository, types: List<ImageTagType>?) = this
        .map { ImageTag(imageRepository = imageRepository, name = it.name) }
        .filter { types == null || it.type in types }
}

@Component
class ImageRepositoryTagResolver : GraphQLResolver<ImageTag> {

    fun image(imageTag: ImageTag, dfe: DataFetchingEnvironment): CompletableFuture<Image?> {
        if (!imageTag.imageRepository.isFullyQualified) {
            return CompletableFuture.supplyAsync {
                null
            }
        }
        return dfe.multipleKeysLoader(ImageTagDataLoader::class).load(imageTag)
    }
}
*/
