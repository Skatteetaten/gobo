package no.skatteetaten.aurora.gobo.graphql.storagegrid

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import no.skatteetaten.aurora.gobo.integration.herkimer.ResourceKind

@Component
class StorageGridTenantDataLoader(
    @Value("\${openshift.cluster}") val cluster: String,
    val herkimerService: HerkimerService
) : GoboDataLoader<String, StorageGridTenant>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, StorageGridTenant> {
        return keys.associateWith { affiliation ->
            val tenantName = getTenantName(affiliation, cluster)
            val tenantResource = herkimerService.getResourceWithClaim(tenantName, ResourceKind.StorageGridTenant)
            StorageGridTenant(
                isRegistered = tenantResource != null
            )
        }
    }
}
