package no.skatteetaten.aurora.gobo.resolvers.route

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.skatteetaten.aurora.gobo.integration.skap.SkapJob

data class Route(
    val websealJobs: List<WebsealJob> = emptyList(),
    val bigipJobs: List<BigipJob> = emptyList()
)

data class WebsealJob(
    val id: String,
    val payload: String,
    val type: String,
    val operation: String,
    val status: String,
    val updated: String,
    val errorMessage: String?,
    val roles: List<String>?,
    val host: String?,
    val routeName: String?
) {

    companion object {

        fun create(skapJob: SkapJob): WebsealJob {
            val mapper = ObjectMapper()

            val payload = mapper.readValue<Map<String, Any>>(skapJob.payload)
            val roles: List<String> by payload.withDefault { emptyList<String>() }
            val host: String by payload.withDefault { null }
            val routeName: String by payload.withDefault { null }

            return WebsealJob(
                id = skapJob.id,
                payload = skapJob.payload,
                type = skapJob.type,
                operation = skapJob.operation,
                status = skapJob.status,
                updated = skapJob.updated,
                errorMessage = skapJob.errorMessage,
                roles = roles,
                host = host,
                routeName = routeName
            )
        }
    }
}

data class BigipJob(
    val id: String,
    val payload: String,
    val type: String,
    val operation: String,
    val status: String,
    val updated: String,
    val errorMessage: String?,
    val asmPolicy: String?,
    val externalHost: String?,
    val apiPaths: List<String>?,
    val oauthScopes: List<String>?,
    val hostname: String?,
    val serviceName: String?
) {

    companion object {

        fun create(skapJob: SkapJob): BigipJob {
            val mapper = ObjectMapper()

            val payload = mapper.readValue<Map<String, Any>>(skapJob.payload)
            val asmPolicy: String by payload.withDefault { null }
            val externalHost: String by payload.withDefault { null }
            val apiPaths: List<String> by payload.withDefault { emptyList<String>() }
            val oauthScopes: List<String> by payload.withDefault { emptyList<String>() }
            val hostname: String by payload.withDefault { null }
            val serviceName: String by payload.withDefault { null }

            return BigipJob(
                id = skapJob.id,
                payload = skapJob.payload,
                type = skapJob.type,
                operation = skapJob.operation,
                status = skapJob.status,
                updated = skapJob.updated,
                errorMessage = skapJob.errorMessage,
                asmPolicy = asmPolicy,
                externalHost = externalHost,
                apiPaths = apiPaths,
                oauthScopes = oauthScopes,
                hostname = hostname,
                serviceName = serviceName
            )
        }
    }
}
