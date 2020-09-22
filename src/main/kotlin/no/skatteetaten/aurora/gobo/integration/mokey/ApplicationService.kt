package no.skatteetaten.aurora.gobo.integration.mokey

import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentRef
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class ApplicationService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {

    suspend fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): List<ApplicationResource> {
        val resources: List<ApplicationResource> = webClient
            .get()
            .uri {
                it.path("/api/application").queryParams(buildQueryParams(affiliations)).build()
            }
            .retrieve()
            .awaitBody()
        return applications?.let { resources.filter { applications.contains(it.name) } } ?: resources
    }

    suspend fun getApplication(id: String): ApplicationResource {
        return webClient
            .get()
            .uri("/api/application/{id}", id)
            .retrieve()
            .awaitBody()
    }

    suspend fun getApplicationDeployment(applicationDeploymentId: String): ApplicationDeploymentResource {
        return webClient
            .get()
            .uri("/api/applicationdeployment/{applicationDeploymentId}", applicationDeploymentId)
            .retrieve()
            .awaitBody()
    }

    suspend fun getApplicationDeployment(applicationDeploymentRefs: List<ApplicationDeploymentRef>): List<ApplicationDeploymentResource> {
        return webClient
            .post()
            .uri("/api/applicationdeployment")
            .body(BodyInserters.fromValue(applicationDeploymentRefs))
            .retrieve()
            .awaitBody()
    }

    suspend fun getApplicationDeploymentDetails(
        token: String,
        applicationDeploymentId: String
    ): ApplicationDeploymentDetailsResource {
        return webClient
            .get()
            .uri("/api/auth/applicationdeploymentdetails/{applicationDeploymentId}", applicationDeploymentId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .awaitBody()
    }

    suspend fun getApplicationDeploymentsForDatabases(
        token: String,
        databaseIds: List<String>
    ): List<ApplicationDeploymentWithDbResource> =
        webClient
            .post()
            .uri("/api/auth/applicationdeploymentbyresource/databases")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .body(BodyInserters.fromValue(databaseIds))
            .retrieve()
            .awaitBody()

    private fun buildQueryParams(affiliations: List<String>): LinkedMultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply { addAll("affiliation", affiliations) }

    suspend fun refreshApplicationDeployment(token: String, refreshParams: RefreshParams) =
        webClient
            .post()
            .uri("/api/auth/refresh")
            .body(BodyInserters.fromValue(refreshParams))
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .awaitBody<Unit>()
}

@Service
class ApplicationServiceBlocking(private val applicationService: ApplicationService) {
    fun getApplications(affiliations: List<String>, applications: List<String>? = null) =
        runBlocking { applicationService.getApplications(affiliations, applications) }

    fun getApplication(id: String): ApplicationResource =
        runBlocking { applicationService.getApplication(id) }

    fun getApplicationDeployment(applicationDeploymentId: String) =
        runBlocking { applicationService.getApplicationDeployment(applicationDeploymentId) }

    fun getApplicationDeployment(applicationDeploymentRefs: List<ApplicationDeploymentRef>) =
        runBlocking { applicationService.getApplicationDeployment(applicationDeploymentRefs) }

    fun getApplicationDeploymentDetails(token: String, applicationDeploymentId: String) =
        runBlocking { applicationService.getApplicationDeploymentDetails(token, applicationDeploymentId) }

    fun refreshApplicationDeployment(token: String, refreshParams: RefreshParams) =
        runBlocking { applicationService.refreshApplicationDeployment(token, refreshParams) }

    fun getApplicationDeploymentsForDatabases(token: String, databaseIds: List<String>) =
        runBlocking { applicationService.getApplicationDeploymentsForDatabases(token, databaseIds) }
}
