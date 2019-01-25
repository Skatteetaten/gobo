package no.skatteetaten.aurora.gobo.integration.dbh

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.createObjectMapper
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration

@Service
class DatabaseSchemaService(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.DBH) private val webClient: WebClient
) {

    fun getDatabaseSchemas(affiliation: String): Mono<List<DatabaseSchemaResource>> {
        val response: Mono<Response<DatabaseSchemaResource>> = webClient
            .get()
            .uri {
                it.path("/api/v1/schema/").queryParam("labels", "affiliation=$affiliation").build()
            }
            .header(HttpHeaders.AUTHORIZATION, "aurora-token ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()

        return response.items()
    }

    fun getDatabaseSchema(id: String): Mono<DatabaseSchemaResource> {
        val response: Mono<Response<DatabaseSchemaResource>> = webClient
            .get()
            .uri("/api/v1/schema/$id")
            .header(HttpHeaders.AUTHORIZATION, "aurora-token ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()

        return response.items().flatMap {
            it.first().toMono()
        }
    }

    fun updateDatabaseSchema(input: SchemaCreationRequest): Mono<Boolean> {
        val response: Mono<Response<DatabaseSchemaResource>> = webClient
            .put()
            .uri("/api/v1/schema/${input.id}")
            .body(BodyInserters.fromObject(input))
            .header(HttpHeaders.AUTHORIZATION, "aurora-token ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()
        return response.flatMap {
            it.success.toMono()
        }
    }

    private fun Mono<Response<DatabaseSchemaResource>>.items() =
        this.flatMap { r ->
            if (!r.success) SourceSystemException(message = r.message, sourceSystem = "dbh").toMono()
            else if (r.count == 0) Mono.empty()
            else r.items.map { item ->
                createObjectMapper().convertValue(
                    item,
                    DatabaseSchemaResource::class.java
                )
            }.filter { dbSchema ->
                dbSchema.containsRequiredLabels()
            }.toMono<List<DatabaseSchemaResource>>()
        }
}

@Service
class DatabaseSchemaServiceBlocking(private val databaseSchemaService: DatabaseSchemaService) {

    fun getDatabaseSchemas(affiliation: String) =
        databaseSchemaService.getDatabaseSchemas(affiliation).blockWithTimeout() ?: emptyList()

    fun getDatabaseSchema(id: String) =
        databaseSchemaService.getDatabaseSchema(id).blockWithTimeout()

    fun updateDatabaseSchema(input: SchemaCreationRequest) =
        databaseSchemaService.updateDatabaseSchema(input).blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockWithTimeout(): T? =
        this.blockAndHandleError(Duration.ofSeconds(30), "dbh")

    private fun <T> Mono<T>.blockNonNullWithTimeout(): T =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "dbh")
}
