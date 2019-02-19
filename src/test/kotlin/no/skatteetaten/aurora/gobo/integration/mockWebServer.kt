package no.skatteetaten.aurora.gobo.integration

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.JsonPath
import no.skatteetaten.aurora.gobo.createObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.hateoas.core.AnnotationRelProvider
import org.springframework.hateoas.hal.HalConfiguration
import org.springframework.hateoas.hal.Jackson2HalModule
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

private fun createTestHateoasObjectMapper() =
    createObjectMapper().apply {
        setHandlerInstantiator(
            Jackson2HalModule.HalHandlerInstantiator(
                AnnotationRelProvider(),
                null,
                null,
                HalConfiguration()
            )
        )
    }

fun MockWebServer.enqueueJson(status: Int = 200, body: Any) {
    val json = body as? String ?: createTestHateoasObjectMapper().writeValueAsString(body)
    val response = MockResponse()
        .setResponseCode(status)
        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
        .setBody(json)
    this.enqueue(response)
}

fun MockWebServer.execute(status: Int, response: Any, fn: () -> Unit): RecordedRequest {
    try {
        this.enqueueJson(status, response)
        fn()
        return this.takeRequest()
    } catch (t: Throwable) {
        this.takeRequest()
        throw t
    }
}

fun MockWebServer.execute(response: MockResponse, fn: () -> Unit): RecordedRequest {
    try {
        this.enqueue(response)
        fn()
        return this.takeRequest()
    } catch (t: Throwable) {
        this.takeRequest()
        throw t
    }
}

fun MockWebServer.execute(response: Any, fn: () -> Unit): RecordedRequest {
    try {
        this.enqueueJson(body = response)
        fn()
        return this.takeRequest()
    } catch (t: Throwable) {
        this.takeRequest()
        throw t
    }
}

fun MockWebServer.execute(vararg responses: Any, fn: () -> Unit): List<RecordedRequest> {
    fun takeRequests() = (1..responses.size).toList().map { this.takeRequest() }

    try {
        responses.forEach { this.enqueueJson(body = it) }
        fn()
        return takeRequests()
    } catch (t: Throwable) {
        takeRequests()
        throw t
    }
}

inline fun <reified T> RecordedRequest.bodyAsObject(path: String = "$"): T {
    val content: Any = JsonPath.parse(String(body.readByteArray())).read(path)
    return jacksonObjectMapper().convertValue(content)
}
