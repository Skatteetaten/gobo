package no.skatteetaten.aurora.gobo.integration.dbh

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.createObjectMapper
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.MissingLabelException
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.publisher.toMono
import java.time.Duration

@Service
class DatabaseSchemaService(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.DBH) private val webClient: WebClient
) {
    companion object {
        const val HEADER_COOLDOWN_DURATION_HOURS = "cooldown-duration-hours"
        const val HEADER_AURORA_TOKEN = "aurora-token"
    }

    fun getDatabaseSchemas(affiliation: String): Mono<List<DatabaseSchemaResource>> {
        val response: Mono<DbhResponse<*>> = webClient
            .get()
            .uri {
                it.path("/api/v1/schema/").queryParam("labels", "affiliation=$affiliation").build()
            }
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()

        return response.items()
    }

    fun getDatabaseSchema(id: String) = webClient
        .get()
        .uri("/api/v1/schema/$id")
        .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .item()

    fun updateDatabaseSchema(input: SchemaUpdateRequest): Mono<DatabaseSchemaResource> {
        val response: Mono<DbhResponse<*>> = webClient
            .put()
            .uri("/api/v1/schema/${input.id}")
            .body(BodyInserters.fromObject(input))
            .header(HttpHeaders.AUTHORIZATION, "aurora-token ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()

        return response.items().flatMap {
            it.first().toMono()
        }
    }

    fun deleteDatabaseSchema(input: SchemaDeletionRequest): Mono<Boolean> {
        val requestSpec = webClient
            .delete()
            .uri("/api/v1/schema/${input.id}")
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")

        input.cooldownDurationHours?.let {
            requestSpec.header(HEADER_COOLDOWN_DURATION_HOURS, it.toString())
        }

        val response: Mono<DbhResponse<*>> = requestSpec.retrieve().bodyToMono()
        return response.blockNonNullAndHandleError(sourceSystem = "dbh").isOk().toMono()
    }

    fun testJdbcConnection(id: String? = null, user: JdbcUser? = null): Mono<Boolean> {
        val response: Mono<DbhResponse<Boolean>> = webClient
            .put()
            .uri("/api/v1/schema/validate")
            .body(
                BodyInserters.fromObject(
                    mapOf(
                        "id" to id,
                        "jdbcUser" to user
                    )
                )
            )
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()

        return response.flatMap {
            it.items.first().toMono()
        }
    }

    fun createDatabaseSchema(input: SchemaCreationRequest): Mono<DatabaseSchemaResource> {
        val missingLabels = input.findMissingOrEmptyLabels()
        if (missingLabels.isNotEmpty()) {
            return Mono.error(MissingLabelException("Missing labels in mutation input: $missingLabels"))
        }

        val response: Mono<DbhResponse<DatabaseSchemaResource>> = webClient
            .post()
            .uri("/api/v1/schema/")
            .body(BodyInserters.fromObject(input))
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()
        return response.flatMap {
            it.items.first().toMono()
        }
    }

    private fun Mono<DbhResponse<*>>.item() = this.items()
        .switchIfEmpty { Mono.empty() }
        .map { it.firstOrNull() }

    private fun Mono<DbhResponse<*>>.items() =
        this.flatMap { r ->
            when {
                r.isFailure() -> {
                    val errorMessage = r.items.firstOrNull() ?: ""
                    SourceSystemException(
                        message = "status=${r.status} error=${errorMessage as String}",
                        sourceSystem = "dbh"
                    ).toMono()
                }
                r.isEmpty() -> Mono.empty()
                else -> r.items.map { item ->
                    createObjectMapper().convertValue(item, DatabaseSchemaResource::class.java)
                }.filter { dbSchema ->
                    dbSchema.containsRequiredLabels()
                }.toMono<List<DatabaseSchemaResource>>()
            }
        }
}

@Service
class DatabaseSchemaServiceBlocking(private val databaseSchemaService: DatabaseSchemaService) {

    fun getDatabaseSchemas(affiliation: String) =
        databaseSchemaService.getDatabaseSchemas(affiliation).blockWithTimeout() ?: emptyList()

    fun getDatabaseSchema(id: String) =
        databaseSchemaService.getDatabaseSchema(id).blockWithTimeout()

    fun updateDatabaseSchema(input: SchemaUpdateRequest) =
        databaseSchemaService.updateDatabaseSchema(input).blockNonNullWithTimeout()

    fun deleteDatabaseSchema(input: SchemaDeletionRequest) =
        databaseSchemaService.deleteDatabaseSchema(input).blockNonNullWithTimeout()

    fun testJdbcConnection(user: JdbcUser) =
        databaseSchemaService.testJdbcConnection(user = user).blockNonNullWithTimeout()

    fun testJdbcConnection(id: String) =
        databaseSchemaService.testJdbcConnection(id = id).blockNonNullWithTimeout()

    fun createDatabaseSchema(input: SchemaCreationRequest) =
        databaseSchemaService.createDatabaseSchema(input).blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockWithTimeout(): T? =
        this.blockAndHandleError(Duration.ofSeconds(30), "dbh")

    private fun <T> Mono<T>.blockNonNullWithTimeout(): T =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "dbh")
}
