package no.skatteetaten.aurora.gobo.resolvers.imagerepository

/*
@Component
class ImageTagListDataLoader(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking
) : KeyDataLoader<ImageRepoDto, TagsDto> {
    override fun getByKey(user: User, key: ImageRepoDto): Try<TagsDto> {
        return Try.tryCall {
            imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(
                imageRepoDto = key,
                token = user.token
            )
        }
    }
}
 */
