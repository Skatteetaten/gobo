package no.skatteetaten.aurora.gobo.integration.dbh

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import no.skatteetaten.aurora.gobo.RequiresDbh
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.resolvers.MissingLabelException
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.database.ConnectionVerificationResponse
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
import reactor.kotlin.core.publisher.toMono

@Service
@ConditionalOnBean(RequiresDbh::class)
class DatabaseServiceReactive(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.DBH) private val webClient: WebClient,
    val objectMapper: ObjectMapper
) {
    companion object {
        const val HEADER_COOLDOWN_DURATION_HOURS = "cooldown-duration-hours"
    }

    fun getDatabaseInstances(): Mono<List<DatabaseInstanceResource>> = webClient
        .get()
        .uri("/api/v1/admin/databaseInstance/")
        .authHeader()
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .items()

    fun getDatabaseSchemas(affiliation: String): Mono<List<DatabaseSchemaResource>> = webClient
        .get()
        .uri {
            it.path("/api/v1/schema/").queryParam("labels", "affiliation=$affiliation").build()
        }
        .authHeader()
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .items()

    fun getDatabaseSchema(id: String): Mono<DatabaseSchemaResource> = webClient
        .get()
        .uri("/api/v1/schema/{id}", id)
        .authHeader()
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .item()

    fun updateDatabaseSchema(input: SchemaUpdateRequest): Mono<DatabaseSchemaResource> = webClient
        .put()
        .uri("/api/v1/schema/{id}", input.id)
        .body(BodyInserters.fromValue(input))
        .authHeader()
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .item()

    fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): Flux<SchemaDeletionResponse> {
        val responses = input.map { request ->
            val requestSpec = webClient
                .delete()
                .uri("/api/v1/schema/{id}", request.id)
                .authHeader()

            request.cooldownDurationHours?.let {
                requestSpec.header(HEADER_COOLDOWN_DURATION_HOURS, it.toString())
            }

            requestSpec.retrieve().bodyToMono<DbhResponse<*>>().map {
                request.id to it
            }
        }

        return Flux.merge(responses).map { SchemaDeletionResponse(id = it.first, success = it.second.isOk()) }
    }

    fun testJdbcConnection(id: String? = null, user: JdbcUser? = null): Mono<ConnectionVerificationResponse> {
        val response: Mono<DbhResponse<ConnectionVerificationResponse>> = webClient
                .put()
                .uri("/api/v1/schema/validate")
                .body(
                        BodyInserters.fromValue(
                                mapOf(
                                        "id" to id,
                                        "jdbcUser" to user
                                )
                        )
                )
                .authHeader()
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
            .body(BodyInserters.fromValue(input))
            .authHeader()
            .retrieve()
            .bodyToMono<DbhResponse<*>>()
            .item()
    }

    private fun WebClient.RequestHeadersSpec<*>.authHeader() =
        this.header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")

    private inline fun <reified T : Any> Mono<DbhResponse<*>>.item(): Mono<T> = this.items<T>().map { it.first() }

    private inline fun <reified T : Any> Mono<DbhResponse<*>>.items(): Mono<List<T>> =
        this.flatMap {
            when {
                it.isFailure() -> onFailure(it)
                it.isEmpty() -> Mono.empty()
                else -> onSuccess<T>(it)
            }
        }

    private inline fun <reified T> onFailure(r: DbhResponse<*>): Mono<List<T>> =
        SourceSystemException(
            message = "status=${r.status} error=${(r.items.firstOrNull() ?: "") as String}",
            sourceSystem = "dbh"
        ).toMono()

    private inline fun <reified T : Any> onSuccess(r: DbhResponse<*>): Mono<List<T>> =
        r.items.map { objectMapper.convertValue(it, T::class.java) }
            .filter {
                if (it is DatabaseSchemaResource) {
                    it.containsRequiredLabels()
                } else {
                    true
                }
            }.toMono()
}

interface DatabaseService {
    fun getDatabaseInstances(): List<DatabaseInstanceResource> = integrationDisabled()
    fun getDatabaseSchemas(affiliation: String): List<DatabaseSchemaResource> = integrationDisabled()
    fun getDatabaseSchema(id: String): DatabaseSchemaResource? = integrationDisabled()
    fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource = integrationDisabled()
    fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaDeletionResponse> = integrationDisabled()
    fun testJdbcConnection(user: JdbcUser): ConnectionVerificationResponse = integrationDisabled()
    fun testJdbcConnection(id: String): ConnectionVerificationResponse = integrationDisabled()
    fun createDatabaseSchema(input: SchemaCreationRequest): DatabaseSchemaResource = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("DBH integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresDbh::class)
class DatabaseServiceBlocking(private val databaseService: DatabaseServiceReactive) :
    DatabaseService {

    override fun getDatabaseInstances() =
        databaseService.getDatabaseInstances().blockWithTimeout() ?: emptyList()

    override fun getDatabaseSchemas(affiliation: String) =
        databaseService.getDatabaseSchemas(affiliation).blockWithTimeout() ?: emptyList()

    override fun getDatabaseSchema(id: String) =
        databaseService.getDatabaseSchema(id).blockWithTimeout()

    override fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource =
        databaseService.updateDatabaseSchema(input).blockNonNullWithTimeout()

    override fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaDeletionResponse> =
        databaseService.deleteDatabaseSchemas(input).collectList().blockNonNullWithTimeout()

    override fun testJdbcConnection(user: JdbcUser) =
            databaseService.testJdbcConnection(user = user).blockNonNullWithTimeout()

    override fun testJdbcConnection(id: String) =
            databaseService.testJdbcConnection(id = id).blockNonNullWithTimeout()

    override fun createDatabaseSchema(input: SchemaCreationRequest) =
        databaseService.createDatabaseSchema(input).blockNonNullWithTimeout()

    private fun <T> Mono<T>.blockWithTimeout(): T? =
        this.blockAndHandleError(Duration.ofSeconds(30), "dbh")

    private fun <T> Mono<T>.blockNonNullWithTimeout(): T =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "dbh")
}

@Service
@ConditionalOnMissingBean(RequiresDbh::class)
class DatabaseServiceDisabled : DatabaseService
