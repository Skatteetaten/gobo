package no.skatteetaten.aurora.gobo.integration.dbh

import no.skatteetaten.aurora.gobo.RequiresDbh
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.createObjectMapper
import no.skatteetaten.aurora.gobo.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.resolvers.MissingLabelException
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration

@Service
@ConditionalOnBean(RequiresDbh::class)
class DatabaseSchemaServiceReactive(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.DBH) private val webClient: WebClient
) {
    companion object {
        const val HEADER_COOLDOWN_DURATION_HOURS = "cooldown-duration-hours"
    }

    fun getDatabaseSchemas(affiliation: String): Mono<List<DatabaseSchemaResource>> = webClient
        .get()
        .uri {
            it.path("/api/v1/schema/").queryParam("labels", "affiliation=$affiliation").build()
        }
        .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .items()

    fun getDatabaseSchema(id: String): Mono<DatabaseSchemaResource> = webClient
        .get()
        .uri("/api/v1/schema/$id")
        .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .item()

    fun getRestorableDatabaseSchemas(affiliation: String): Mono<List<RestorableDatabaseSchemaResource>> = webClient
        .get()
        .uri {
            it.path("/api/v1/restorableSchema/").queryParam("labels", "affiliation=$affiliation").build()
        }
        .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .items()

    fun updateDatabaseSchema(input: SchemaUpdateRequest): Mono<DatabaseSchemaResource> = webClient
        .put()
        .uri("/api/v1/schema/${input.id}")
        .body(BodyInserters.fromObject(input))
        .header(HttpHeaders.AUTHORIZATION, "aurora-token ${sharedSecretReader.secret}")
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .item()

    fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): Flux<SchemaDeletionResponse> {
        val responses = input.map { request ->
            val requestSpec = webClient
                .delete()
                .uri("/api/v1/schema/${request.id}")
                .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")

            request.cooldownDurationHours?.let {
                requestSpec.header(HEADER_COOLDOWN_DURATION_HOURS, it.toString())
            }

            requestSpec.retrieve().bodyToMono<DbhResponse<*>>().map {
                request.id to it
            }
        }

        return Flux.merge(responses).map { SchemaDeletionResponse(id = it.first, success = it.second.isOk()) }
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

        return webClient
            .post()
            .uri("/api/v1/schema/")
            .body(BodyInserters.fromObject(input))
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono<DbhResponse<*>>()
            .item()
    }
}

private inline fun <reified T : ResourceValidator> Mono<DbhResponse<*>>.item() = this.items<T>().map { it.first() }

private inline fun <reified T : ResourceValidator> Mono<DbhResponse<*>>.items(): Mono<List<T>> =
    this.flatMap {
        when {
            it.isFailure() -> onFailure(it)
            it.isEmpty() -> Mono.empty<List<T>>()
            else -> onSuccess(it)
        }
    }

private inline fun <reified T : ResourceValidator> onFailure(r: DbhResponse<*>): Mono<List<T>> {
    return SourceSystemException(
        message = "status=${r.status} error=${(r.items.firstOrNull() ?: "") as String}",
        sourceSystem = "dbh"
    ).toMono()
}

private inline fun <reified T : ResourceValidator> onSuccess(r: DbhResponse<*>): Mono<List<T>> =
    r.items
        .map { createObjectMapper().convertValue(it, T::class.java) }
        .filter { it.valid }
        .toMono()

interface DatabaseSchemaService {
    fun getDatabaseSchemas(affiliation: String): List<DatabaseSchemaResource> = integrationDisabled()
    fun getDatabaseSchema(id: String): DatabaseSchemaResource? = integrationDisabled()
    fun getRestorableDatabaseSchemas(affiliation: String): List<RestorableDatabaseSchemaResource> =
        integrationDisabled()

    fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource = integrationDisabled()
    fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaDeletionResponse> = integrationDisabled()
    fun testJdbcConnection(user: JdbcUser): Boolean = integrationDisabled()
    fun testJdbcConnection(id: String): Boolean = integrationDisabled()
    fun createDatabaseSchema(input: SchemaCreationRequest): DatabaseSchemaResource = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("DBH integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresDbh::class)
class DatabaseSchemaServiceBlocking(private val databaseSchemaService: DatabaseSchemaServiceReactive) :
    DatabaseSchemaService {

    override fun getDatabaseSchemas(affiliation: String) =
        databaseSchemaService.getDatabaseSchemas(affiliation).blockWithTimeout() ?: emptyList()

    override fun getDatabaseSchema(id: String) =
        databaseSchemaService.getDatabaseSchema(id).blockWithTimeout()

    override fun getRestorableDatabaseSchemas(affiliation: String) =
        databaseSchemaService.getRestorableDatabaseSchemas(affiliation).blockWithTimeout() ?: emptyList()

    override fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource =
        databaseSchemaService.updateDatabaseSchema(input).blockNonNullWithTimeout()

    override fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaDeletionResponse> =
        databaseSchemaService.deleteDatabaseSchemas(input).collectList().blockNonNullWithTimeout()

    override fun testJdbcConnection(user: JdbcUser) =
        databaseSchemaService.testJdbcConnection(user = user).blockNonNullWithTimeout()

    override fun testJdbcConnection(id: String) =
        databaseSchemaService.testJdbcConnection(id = id).blockNonNullWithTimeout()

    override fun createDatabaseSchema(input: SchemaCreationRequest) =
        databaseSchemaService.createDatabaseSchema(input).blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockWithTimeout(): T? =
        this.blockAndHandleError(Duration.ofSeconds(30), "dbh")

    private fun <T> Mono<T>.blockNonNullWithTimeout(): T =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "dbh")
}

@Service
@ConditionalOnMissingBean(RequiresDbh::class)
class DatabaseSchemaServiceDisabled : DatabaseSchemaService