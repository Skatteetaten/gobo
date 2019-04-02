package no.skatteetaten.aurora.gobo.integration

import assertk.Assert
import assertk.assertions.support.expected
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpHeaders

fun Assert<RecordedRequest>.containsAuroraToken() = given { request ->
    request.headers[HttpHeaders.AUTHORIZATION]?.let {
        if (it.startsWith(HEADER_AURORA_TOKEN)) return
    }
    expected("Authorization header to contain $HEADER_AURORA_TOKEN")
}

fun Assert<List<RecordedRequest>>.containsAuroraTokens() = given { requests ->
    val tokens = requests.filter { request ->
        request.headers[HttpHeaders.AUTHORIZATION]?.startsWith(HEADER_AURORA_TOKEN) ?: false
    }
    if (tokens.size == requests.size) return
    expected("Authorization header to contain $HEADER_AURORA_TOKEN")
}