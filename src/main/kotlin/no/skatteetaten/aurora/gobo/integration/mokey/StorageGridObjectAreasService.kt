package no.skatteetaten.aurora.gobo.integration.mokey

import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.awaitWithRetry
import uk.q3c.rest.hal.HalResource

@JsonIgnoreProperties(ignoreUnknown = true)
data class StoragegridObjectAreaResource(
    val name: String,
    val namespace: String,
    val creationTimestamp: String,
    val objectArea: String,
    val bucketName: String,
    val message: String,
    val reason: String,
    val success: Boolean,
) : HalResource()

@Service
class StorageGridObjectAreasService(@TargetService(ServiceTypes.MOKEY) val webClient: WebClient) {
    suspend fun getObjectAreas(affiliation: String, token: String): List<StoragegridObjectAreaResource> {
        return webClient.get()
            .uri("/api/auth/storagegridobjectarea?affiliation={affiliation}", affiliation)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .handleHttpStatusErrors(affiliation)
            .awaitWithRetry()
    }
}
