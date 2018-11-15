package no.skatteetaten.aurora.gobo.resolvers.userSettings

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentFilterService
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class UserSettingsQueryResolver(private val applicationDeploymentFilterService: ApplicationDeploymentFilterService) :
    GraphQLQueryResolver {

    fun getUserSettings(dfe: DataFetchingEnvironment): UserSettings {
        val filters = applicationDeploymentFilterService.getFilters(dfe.currentUser().token)
        return UserSettings(
            filters.map { ApplicationDeploymentFilter(it.name, it.affiliation, it.applications, it.environments) }
        )
    }
}