package no.skatteetaten.aurora.gobo.graphql.imagerepository

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.GoboException
import org.springframework.stereotype.Component

@Component
class ImageRepositoryQueryResolver : Query {

    suspend fun imageRepositories(repositories: List<String>, dfe: DataFetchingEnvironment): List<ImageRepository> {
// FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot access imagrepositories")
        if (repositories.isEmpty()) throw GoboException("repositories is empty")

        return repositories.map { ImageRepository.fromRepoString(it) }
    }
}
