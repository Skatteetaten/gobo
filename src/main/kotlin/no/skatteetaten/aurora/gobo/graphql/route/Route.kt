package no.skatteetaten.aurora.gobo.graphql.route

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
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
    @GraphQLIgnore
    val roles: List<String>?,
    val host: String?,
    val routeName: String?
) {

    fun roles() = roles.toString()

    companion object {

        fun create(skapJob: SkapJob): WebsealJob {
            val mapper = ObjectMapper()

            val payload = mapper.readValue<Map<String, Any>>(skapJob.payload)
            val roles: List<String> by payload.withDefaultEmptyList()
            val host: String by payload.withDefaultNull()
            val routeName: String by payload.withDefaultNull()

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
    val serviceName: String?,
    val name: String?
) {

    companion object {

        fun create(skapJob: SkapJob): BigipJob {
            val mapper = ObjectMapper()

            val payload = mapper.readValue<Map<String, Any>>(skapJob.payload)
            val asmPolicy: String by payload.withDefaultNull()
            val externalHost: String by payload.withDefaultNull()
            val apiPaths: List<String> by payload.withDefaultEmptyList()
            val oauthScopes: List<String> by payload.withDefaultEmptyList()
            val hostname: String by payload.withDefaultNull()
            val serviceName: String by payload.withDefaultNull()
            val name: String by payload.withDefaultNull()

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
                serviceName = serviceName,
                name = name
            )
        }
    }
}

private fun Map<String, Any>.withDefaultEmptyList() = this.withDefault { emptyList<String>() }
private fun Map<String, Any>.withDefaultNull() = this.withDefault { null }
