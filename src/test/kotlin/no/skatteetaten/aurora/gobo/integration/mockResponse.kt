package no.skatteetaten.aurora.gobo.integration

import okhttp3.mockwebserver.MockResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

fun createJsonMockResponse(status: Int = 200, body: String = "") =
        MockResponse()
                .setResponseCode(status)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .setBody(body)
