package no.skatteetaten.aurora.gobo.graphql.usersettings

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.graphql.token
import org.springframework.stereotype.Component

@Component
class UserSettingsQuery(private val userSettingsService: UserSettingsService) : Query {
    @GraphQLDescription("Get user settings")
    suspend fun userSettings(dfe: DataFetchingEnvironment): UserSettings {
        val settings = userSettingsService.getUserSettings(dfe.token())
        return UserSettings(settings)
    }
}
