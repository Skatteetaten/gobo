package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class ImageRepositoryQueryResolver : GraphQLQueryResolver {

    fun getImageRepositories(repositories: List<String>) =
        repositories.map { ImageRepository.fromRepoString(it) }
}