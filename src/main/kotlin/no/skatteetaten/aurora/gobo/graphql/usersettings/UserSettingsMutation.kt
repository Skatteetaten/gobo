package no.skatteetaten.aurora.gobo.graphql.usersettings

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import org.springframework.stereotype.Component

@Component
class UserSettingsMutation(private val userSettingsService: UserSettingsService) : Mutation {
    @GraphQLDescription("Update user settings")
    suspend fun updateUserSettings(input: UserSettingsInput, dfe: DataFetchingEnvironment): Boolean {
        userSettingsService.updateUserSettings(dfe.token(), input)
        return true
    }
}
