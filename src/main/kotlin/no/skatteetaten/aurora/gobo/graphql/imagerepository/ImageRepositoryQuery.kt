package no.skatteetaten.aurora.gobo.graphql.imagerepository

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class ImageRepositoryQuery : Query {

    suspend fun imageRepositories(repositories: List<String>, dfe: DataFetchingEnvironment): List<ImageRepository> {
        dfe.checkValidUserToken()
        if (repositories.isEmpty()) throw GoboException("repositories is empty")
        return repositories.map { ImageRepository.fromRepoString(it) }
    }
}
