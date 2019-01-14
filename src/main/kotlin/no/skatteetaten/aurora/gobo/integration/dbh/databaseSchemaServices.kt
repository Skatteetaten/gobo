package no.skatteetaten.aurora.gobo.integration.dbh

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.createObjectMapper
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration

@Service
class DatabaseSchemaService(
    @Value("\${dbh.token}") private val token: String,
    @TargetService(ServiceTypes.DBH) private val webClient: WebClient
) {

    fun getDatabaseSchemas(affiliation: String): Mono<List<DatabaseSchemaResource>> {
        val response: Mono<Response<DatabaseSchemaResource>> = webClient
            .get()
            .uri {
                it.path("/api/v1/schema/").queryParam("labels", "affiliation=$affiliation").build()
            }
            .header(HttpHeaders.AUTHORIZATION, "aurora-token $token")
            .retrieve()
            .bodyToMono()

        return response.items()
    }

    fun getDatabaseSchema(id: String): Mono<List<DatabaseSchemaResource>> {
        val response: Mono<Response<DatabaseSchemaResource>> = webClient
            .get()
            .uri("/api/v1/schema/$id")
            .header(HttpHeaders.AUTHORIZATION, "aurora-token $token")
            .retrieve()
            .bodyToMono()

        return response.items()
    }

    private inline fun <reified T> Mono<Response<T>>.items() =
        this.flatMap { r ->
            if (!r.success) SourceSystemException(message = r.message, sourceSystem = "dbh").toMono()
            else if (r.count == 0) Mono.empty()
            else r.items.map { item ->
                createObjectMapper().convertValue(
                    item,
                    T::class.java
                )
            }.toMono<List<T>>()
        }
}

@Service
class DatabaseSchemaServiceBlocking(private val databaseSchemaService: DatabaseSchemaService) {

    fun getDatabaseSchemas(affiliation: String) =
        databaseSchemaService.getDatabaseSchemas(affiliation).blockNonNullWithTimeout()

    fun getDatabaseSchema(id: String) =
        databaseSchemaService.getDatabaseSchema(id).blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockNonNullWithTimeout() =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "dbh")
}
