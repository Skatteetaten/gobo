package no.skatteetaten.aurora.gobo.graphql.imagerepository

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import org.springframework.stereotype.Component

data class IsLatestDigestKey(val digest: String?, val imageTag: ImageTag)

@Component
class IsLatestDigestDataLoader(private val imageRegistryService: ImageRegistryService) : GoboDataLoader<IsLatestDigestKey, Boolean>() {
    override suspend fun getByKeys(keys: Set<IsLatestDigestKey>, ctx: GraphQLContext): Map<IsLatestDigestKey, Boolean> {
        return keys.associateWith {
            val imageTag = it.imageTag

            val imageRepoDto = imageTag.imageRepository.toImageRepo()
            val imageTagDto = imageRegistryService.findImageTagDto(
                imageRepoDto,
                imageTag.name,
                token = ctx.token
            )
            it.digest == imageTagDto.dockerDigest
        }
    }
}
