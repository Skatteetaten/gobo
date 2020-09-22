package no.skatteetaten.aurora.gobo.integration.dbh

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.RequiresDbh
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.resolvers.MissingLabelException
import no.skatteetaten.aurora.gobo.resolvers.database.ConnectionVerificationResponse
import no.skatteetaten.aurora.gobo.resolvers.database.JdbcUser
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
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
    @TargetService(ServiceTypes.DBH) private val webClient: WebClient,
    val objectMapper: ObjectMapper
) {
    companion object {
        const val HEADER_COOLDOWN_DURATION_HOURS = "cooldown-duration-hours"
    }

    suspend fun getDatabaseInstances(): List<DatabaseInstanceResource> = webClient
        .get()
        .uri("/api/v1/admin/databaseInstance/")
        .retrieveItems()

    suspend fun getDatabaseSchemas(affiliation: String): List<DatabaseSchemaResource> = webClient
        .get()
        .uri {
            it.path("/api/v1/schema/").queryParam("labels", "affiliation=$affiliation").build()
        }
        .retrieveItems()

    suspend fun getDatabaseSchema(id: String): DatabaseSchemaResource = webClient
        .get()
        .uri("/api/v1/schema/{id}", id)
        .retrieveItem()

    suspend fun getRestorableDatabaseSchemas(affiliation: String): List<RestorableDatabaseSchemaResource> = webClient
        .get()
        .uri {
            it.path("/api/v1/restorableSchema/").queryParam("labels", "affiliation=$affiliation").build()
        }
        .retrieveItems()

    suspend fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource = webClient
        .put()
        .uri("/api/v1/schema/{id}", input.id)
        .body(BodyInserters.fromValue(input))
        .retrieveItem()

    suspend fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaCooldownChangeResponse> {
        val responses = input.map { request ->
            val requestSpec = webClient
                .delete()
                .uri("/api/v1/schema/{id}", request.id)

            request.cooldownDurationHours?.let {
                requestSpec.header(HEADER_COOLDOWN_DURATION_HOURS, it.toString())
            }

            requestSpec.retrieve().bodyToDbhResponse().map {
                request.id to it
            }
        }

        return Flux.merge(responses).map { SchemaCooldownChangeResponse(id = it.first, success = it.second.isOk()) }.collectList().awaitFirst()
    }

    suspend fun restoreDatabaseSchemas(input: List<SchemaRestorationRequest>): List<SchemaCooldownChangeResponse> {
        val responses = input.map { request ->
            webClient
                .patch()
                .uri("/api/v1/restorableSchema/${request.id}")
                .body(
                    BodyInserters.fromValue(
                        mapOf(
                            "active" to request.active
                        )
                    )
                )
                .retrieve()
                .bodyToDbhResponse()
                .map {
                    request.id to it
                }
        }

        return Flux.merge(responses).map { SchemaCooldownChangeResponse(id = it.first, success = it.second.isOk()) }.collectList().awaitFirst()
    }

    suspend fun testJdbcConnection(id: String? = null, user: JdbcUser? = null): ConnectionVerificationResponse =
        webClient
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
            .retrieveItem()

    suspend fun createDatabaseSchema(input: SchemaCreationRequest): DatabaseSchemaResource {
        val missingLabels = input.findMissingOrEmptyLabels()
        if (missingLabels.isNotEmpty()) {
            throw MissingLabelException("Missing labels in mutation input: $missingLabels")
        }

        return webClient
            .post()
            .uri("/api/v1/schema/")
            .body(BodyInserters.fromValue(input))
            .retrieveItem()
    }

    private suspend inline fun <reified T> WebClient.RequestHeadersSpec<*>.retrieveItem() =
        retrieveItems<T>().first()

    private suspend inline fun <reified T> WebClient.RequestHeadersSpec<*>.retrieveItems(): List<T> =
        this.retrieve().bodyToDbhResponse().items<T>().awaitFirst()!!

    private inline fun <reified T> Mono<DbhResponse<*>>.items(): Mono<List<T>> =
        this.flatMap {
            when {
                it.isFailure() -> onFailure(it)
                it.isEmpty() -> Mono.empty<List<T>>()
                else -> onSuccess<T>(it)
            }
        }

    private inline fun <reified T> onFailure(r: DbhResponse<*>): Mono<List<T>> =
        SourceSystemException(
            message = "status=${r.status} error=${(r.items.firstOrNull() ?: "") as String}",
            sourceSystem = "dbh"
        ).toMono()

    private inline fun <reified T> onSuccess(r: DbhResponse<*>): Mono<List<T>> =
        r.items
            .map { objectMapper.convertValue(it, T::class.java) }
            .filter {
                when (it) {
                    is ResourceValidator -> it.valid
                    else -> true
                }
            }
            .toMono()
}

private fun WebClient.ResponseSpec.bodyToDbhResponse() = this.bodyToMono<DbhResponse<*>>()

interface DatabaseService {
    fun getDatabaseInstances(): List<DatabaseInstanceResource> = integrationDisabled()
    fun getDatabaseSchemas(affiliation: String): List<DatabaseSchemaResource> = integrationDisabled()
    fun getDatabaseSchema(id: String): DatabaseSchemaResource? = integrationDisabled()
    fun getRestorableDatabaseSchemas(affiliation: String): List<RestorableDatabaseSchemaResource> =
        integrationDisabled()

    fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource = integrationDisabled()
    fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaCooldownChangeResponse> =
        integrationDisabled()

    fun restoreDatabaseSchemas(input: List<SchemaRestorationRequest>): List<SchemaCooldownChangeResponse> =
        integrationDisabled()

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
        runBlocking { databaseService.getDatabaseInstances() }

    override fun getDatabaseSchemas(affiliation: String) =
        runBlocking { databaseService.getDatabaseSchemas(affiliation) }

    override fun getDatabaseSchema(id: String) =
        runBlocking { databaseService.getDatabaseSchema(id) }

    override fun getRestorableDatabaseSchemas(affiliation: String) =
        runBlocking { databaseService.getRestorableDatabaseSchemas(affiliation) }

    override fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource =
        runBlocking { databaseService.updateDatabaseSchema(input) }

    override fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaCooldownChangeResponse> =
        runBlocking { databaseService.deleteDatabaseSchemas(input) }

    override fun restoreDatabaseSchemas(input: List<SchemaRestorationRequest>): List<SchemaCooldownChangeResponse> =
        runBlocking { databaseService.restoreDatabaseSchemas(input) }

    override fun testJdbcConnection(user: JdbcUser) =
        runBlocking { databaseService.testJdbcConnection(user = user) }

    override fun testJdbcConnection(id: String) =
        runBlocking { databaseService.testJdbcConnection(id = id) }

    override fun createDatabaseSchema(input: SchemaCreationRequest) =
        runBlocking { databaseService.createDatabaseSchema(input) }
}

@Service
@ConditionalOnMissingBean(RequiresDbh::class)
class DatabaseServiceDisabled : DatabaseService
