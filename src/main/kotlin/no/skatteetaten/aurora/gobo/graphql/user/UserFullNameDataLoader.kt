package no.skatteetaten.aurora.gobo.graphql.user

import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.security.UNKNOWN_USER_NAME
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import org.springframework.stereotype.Component

@Component
class UserFullNameDataLoader(private val kubernetesClient: KubernetesCoroutinesClient) : GoboDataLoader<String, String>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GoboGraphQLContext): Map<String, String> {
        return keys.associateWith {
            kubernetesClient.currentUser(it)?.fullName ?: UNKNOWN_USER_NAME
        }
    }
}
