package no.skatteetaten.aurora.gobo.resolvers.usersettings

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsService
import no.skatteetaten.aurora.gobo.security.currentUser
import org.springframework.stereotype.Component

@Component
class UserSettingsQueryResolver(private val userSettingsService: UserSettingsService) : Query {

    fun getUserSettings(dfe: DataFetchingEnvironment): UserSettings {
        val settings = userSettingsService.getUserSettings(dfe.currentUser().token)
        return UserSettings(settings)
    }
}

@Component
class UserSettingsMutationResolver(private val userSettingsService: UserSettingsService) : Mutation {
    fun updateUserSettings(input: UserSettings, dfe: DataFetchingEnvironment): Boolean {
        userSettingsService.updateUserSettings(dfe.currentUser().token, input)
        return true
    }
}
