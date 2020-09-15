package no.skatteetaten.aurora.gobo.resolvers.usersettings

import com.expediagroup.graphql.spring.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class UserSettingsMutation(
        private val userSettingsService: UserSettingsService
) : Mutation {
    fun updateUserSettings(userSettings: UserSettings, dfe: DataFetchingEnvironment): Boolean {
        userSettingsService.updateUserSettings(dfe.currentUser().token, userSettings)
        return true
    }
}
