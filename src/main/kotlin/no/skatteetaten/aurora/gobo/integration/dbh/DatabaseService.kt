package no.skatteetaten.aurora.gobo.integration.dbh

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirst
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.RequiresDbh
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.database.ConnectionVerificationResponse
import no.skatteetaten.aurora.gobo.graphql.database.JdbcUser
import no.skatteetaten.aurora.gobo.integration.onStatusNotOk
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

private val logger = KotlinLogging.logger { }

@Service
@ConditionalOnBean(RequiresDbh::class)
class DatabaseServiceReactive(
    @TargetService(ServiceTypes.DBH) private val webClient: WebClient,
    val objectMapper: ObjectMapper
) : DatabaseService {
    companion object {
        const val HEADER_COOLDOWN_DURATION_HOURS = "cooldown-duration-hours"
    }

    override suspend fun getDatabaseInstances(): List<DatabaseInstanceResource> = webClient
        .get()
        .uri("/api/v1/admin/databaseInstance/")
        .retrieveItems()

    override suspend fun getDatabaseSchemas(affiliation: String): List<DatabaseSchemaResource> = webClient
        .get()
        .uri("/api/v1/schema/?labels={labels}", "affiliation=$affiliation")
        .retrieveItems()

    override suspend fun getDatabaseSchema(id: String): DatabaseSchemaResource = webClient
        .get()
        .uri("/api/v1/schema/{id}", id)
        .retrieveItem()

    override suspend fun getRestorableDatabaseSchemas(affiliation: String): List<RestorableDatabaseSchemaResource> =
        webClient
            .get()
            .uri("/api/v1/restorableSchema/?labels={labels}", "affiliation=$affiliation")
            .retrieveItems()

    override suspend fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource = webClient
        .put()
        .uri("/api/v1/schema/{id}", input.id)
        .body(BodyInserters.fromValue(input))
        .retrieveItem()

    override suspend fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaCooldownChangeResponse> {
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

        return Flux.merge(responses).map { SchemaCooldownChangeResponse(id = it.first, success = it.second.isOk()) }
            .collectList().awaitFirst()
    }

    override suspend fun restoreDatabaseSchemas(input: List<SchemaRestorationRequest>): List<SchemaCooldownChangeResponse> {
        val responses = input.map { request ->
            webClient
                .patch()
                .uri("/api/v1/restorableSchema/{id}", request.id)
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

        return Flux.merge(responses).map { SchemaCooldownChangeResponse(id = it.first, success = it.second.isOk()) }
            .collectList().awaitFirst()
    }

    override suspend fun testJdbcConnection(user: JdbcUser): ConnectionVerificationResponse {
        return testJdbcConnectionInternal(user = user)
    }

    override suspend fun testJdbcConnection(id: String): ConnectionVerificationResponse {
        return testJdbcConnectionInternal(id = id)
    }

    private suspend fun testJdbcConnectionInternal(
        id: String? = null,
        user: JdbcUser? = null
    ): ConnectionVerificationResponse =
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

    override suspend fun createDatabaseSchema(input: SchemaCreationRequest): DatabaseSchemaResource {
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
        this.retrieve()
            .onStatusNotOk { status, body ->
                DbhIntegrationException(
                    message = "Request failed for database resource",
                    integrationResponse = body,
                    status = status
                )
            }
            .bodyToDbhResponse().items<T>().defaultIfEmpty(emptyList()).awaitFirst()!!

    private inline fun <reified T> Mono<DbhResponse<*>>.items(): Mono<List<T>> =
        this.flatMap {
            when {
                it.isFailure() -> onFailure(it)
                it.isEmpty() -> Mono.empty()
                else -> onSuccess(it)
            }
        }

    private inline fun <reified T> onFailure(r: DbhResponse<*>): Mono<List<T>> =
        DbhIntegrationException(
            message = "status=${r.status} error=${(r.items.firstOrNull() ?: "") as String}",
            integrationResponse = r.toString()
        ).toMono()

    private inline fun <reified T> onSuccess(r: DbhResponse<*>): Mono<List<T>> =
        r.items
            .map { item ->
                runCatching {
                    objectMapper.convertValue(item, T::class.java)
                }.onFailure {
                    logger.error(it) { "Unable to parse response items from dbh: $item" }
                }.getOrThrow()
            }
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
    suspend fun getDatabaseInstances(): List<DatabaseInstanceResource> = integrationDisabled()
    suspend fun getDatabaseSchemas(affiliation: String): List<DatabaseSchemaResource> = integrationDisabled()
    suspend fun getDatabaseSchema(id: String): DatabaseSchemaResource = integrationDisabled()
    suspend fun getRestorableDatabaseSchemas(affiliation: String): List<RestorableDatabaseSchemaResource> =
        integrationDisabled()

    suspend fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource = integrationDisabled()
    suspend fun deleteDatabaseSchemas(input: List<SchemaDeletionRequest>): List<SchemaCooldownChangeResponse> =
        integrationDisabled()

    suspend fun restoreDatabaseSchemas(input: List<SchemaRestorationRequest>): List<SchemaCooldownChangeResponse> =
        integrationDisabled()

    suspend fun testJdbcConnection(user: JdbcUser): ConnectionVerificationResponse = integrationDisabled()
    suspend fun testJdbcConnection(id: String): ConnectionVerificationResponse = integrationDisabled()
    suspend fun createDatabaseSchema(input: SchemaCreationRequest): DatabaseSchemaResource = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("DBH integration is disabled for this environment")
}

@Service
@ConditionalOnMissingBean(RequiresDbh::class)
class DatabaseServiceDisabled : DatabaseService
