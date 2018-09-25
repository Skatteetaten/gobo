package no.skatteetaten.aurora.gobo.integration

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

fun MockWebServer.enqueueJson(status: Int = 200, body: Any) {
    this.enqueueJson(status, createTestHateoasObjectMapper().writeValueAsString(body))
}

fun MockWebServer.enqueueJson(status: Int = 200, body: String = "") {
    val response = MockResponse()
        .setResponseCode(status)
        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
        .setBody(body)
    this.enqueue(response)
}