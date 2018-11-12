package no.skatteetaten.aurora.gobo.resolvers.userSettings

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class UserSettingsQueryResolver : GraphQLQueryResolver {

    fun getUserSettings(): UserSettings {
        return UserSettings(applicationDeploymentFilters = listOf(
            ApplicationDeploymentFilter("paas")
        ))
    }

}