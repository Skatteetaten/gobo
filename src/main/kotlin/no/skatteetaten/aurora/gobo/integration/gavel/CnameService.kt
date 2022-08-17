package no.skatteetaten.aurora.gobo.integration.gavel

import no.skatteetaten.aurora.gobo.RequiresGavel
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.cname.CnameInfo
import no.skatteetaten.aurora.gobo.integration.awaitWithRetry
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

interface CnameService {
    suspend fun getCnameInfo(): List<CnameInfo> = integrationDisabled()

    suspend fun getCnameInfo(affiliation: String): List<CnameInfo> = getCnameInfo()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Gavel integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresGavel::class)
class CnameServiceReactive(@TargetService(ServiceTypes.GAVEL) val webClient: WebClient) : CnameService {
    override suspend fun getCnameInfo(): List<CnameInfo> =
        webClient
            .get()
            .uri("/admin/info/job/v1")
            .retrieve()
            .awaitWithRetry()

    override suspend fun getCnameInfo(affiliation: String): List<CnameInfo> = getCnameInfo().filter {
        it.containsAffiliation(listOf(affiliation))
    }
}

@Service
@ConditionalOnMissingBean(RequiresGavel::class)
class CnameServiceDisabled : CnameService
