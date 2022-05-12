package no.skatteetaten.aurora.gobo.graphql.storagegrid

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import no.skatteetaten.aurora.gobo.integration.herkimer.ResourceKind

@Component
class StoragegridTenantDataLoader(
    @Value("\${openshift.cluster}") val cluster: String,
    val herkimerService: HerkimerService
) : GoboDataLoader<String, StoragegridTenant>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, StoragegridTenant> {
        return keys.associateWith { affiliation ->
            val tenantName = getTenantName(affiliation, cluster)
            val tenantResource = kotlin.runCatching { herkimerService.getResourceWithClaim(tenantName, ResourceKind.StorageGridTenant) }.getOrThrow()
            StoragegridTenant(
                isRegistered = tenantResource != null
            )
        }
    }
}
