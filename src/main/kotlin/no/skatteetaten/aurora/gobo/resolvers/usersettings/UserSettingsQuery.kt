package no.skatteetaten.aurora.gobo.resolvers.usersettings

import com.expediagroup.graphql.annotations.GraphQLDescription
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class UserSettingsQuery(private val userSettingsService: UserSettingsService) : Query {
    @GraphQLDescription("Get user settings")
    suspend fun userSettings(dfe: DataFetchingEnvironment): UserSettings {
        val settings = userSettingsService.getUserSettings(dfe.currentUser().token)
        return UserSettings(settings)
    }
}