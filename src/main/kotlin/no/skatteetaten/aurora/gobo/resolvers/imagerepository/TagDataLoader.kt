package no.skatteetaten.aurora.gobo.resolvers.imagerepository

class TagDataLoader {

}


/*

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


 */