package no.skatteetaten.aurora.gobo.integration.mokey

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.reactive.function.client.WebClient
import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.jsonResponse
import okhttp3.mockwebserver.MockWebServer

class StoragegridObjectAreaServiceTest {
    private val server = MockWebServer()
    private val url = server.url("/")
    private val storagegridObjectAreasService = StoragegridObjectAreasService(WebClient.create(url.toString()))

    @ParameterizedTest
    @ValueSource(ints = [400, 401, 403, 404, 418, 500, 501])
    fun `Should handle http errors`(statusCode: Int) {
        val response = StoragegridObjectAreaResource(
            name = "test",
            namespace = "aup",
            creationTimestamp = "today",
            objectArea = "area",
            bucketName = "aup-utv04-default",
            message = "msg",
            reason = "reason",
            success = true
        )
        val mockResponse = jsonResponse(response)
            .setResponseCode(statusCode)

        server.executeBlocking(mockResponse) {
            val mokeyIntegrationsExceptionAssert = assertThat {
                storagegridObjectAreasService.getObjectAreas("aup", "token")
            }.isNotNull()
                .isFailure()
                .isInstanceOf(MokeyIntegrationException::class)

            if (statusCode != 404) {
                mokeyIntegrationsExceptionAssert.messageContains("Downstream request failed")
            } else {
                mokeyIntegrationsExceptionAssert.messageContains("The requested resource was not found")
            }
        }
    }
}
