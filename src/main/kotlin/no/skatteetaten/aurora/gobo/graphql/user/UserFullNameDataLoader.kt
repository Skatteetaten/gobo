package no.skatteetaten.aurora.gobo.graphql.user

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.security.UNKNOWN_USER_NAME
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import org.springframework.stereotype.Component

data class UserFullName(val name: String?)

@Component
class UserFullNameDataLoader(private val kubernetesClient: KubernetesCoroutinesClient) :
    KeyDataLoader<String, UserFullName> {

    override suspend fun getByKey(key: String, context: GoboGraphQLContext): UserFullName {
        val name = kubernetesClient.currentUser(key)?.fullName ?: UNKNOWN_USER_NAME
        return UserFullName(name)
    }
}
