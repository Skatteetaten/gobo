package no.skatteetaten.aurora.gobo.integration.unclematt

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbeResult(
    val result: Result?,
    val podIp: String?,
    val hostIp: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Result(
    val status: ProbeStatus,
    val message: String?,
    val dnsname: String?,
    val resolvedIp: String?,
    val port: String?
) {
    companion object {
        fun unknownResult() = Result(ProbeStatus.UNKNOWN, null, null, null, null)
    }
}

enum class ProbeStatus {
    ERROR,
    DNS_FAILED,
    DNS_SUCCESS,
    OPEN,
    CLOSED,
    FILTERED,
    @JsonEnumDefaultValue
    UNKNOWN
}
