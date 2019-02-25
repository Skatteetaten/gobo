package no.skatteetaten.aurora.gobo.integration.dbh

import no.skatteetaten.aurora.gobo.RequiresDbh
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.createObjectMapper
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

    fun updateDatabaseSchema(input: SchemaUpdateRequest) = webClient
        .put()
        .uri("/api/v1/schema/${input.id}")
        .body(BodyInserters.fromObject(input))
        .header(HttpHeaders.AUTHORIZATION, "aurora-token ${sharedSecretReader.secret}")
        .retrieve()
        .bodyToMono<DbhResponse<*>>()
        .item()

    fun deleteDatabaseSchema(input: SchemaDeletionRequest): Mono<Boolean> {
        val requestSpec = webClient
            .delete()
            .uri("/api/v1/schema/${input.id}")
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")

        input.cooldownDurationHours?.let {
            requestSpec.header(HEADER_COOLDOWN_DURATION_HOURS, it.toString())
        }

        val response: Mono<DbhResponse<*>> = requestSpec.retrieve().bodyToMono()
        return response.map { it.isOk() }
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

    private fun Mono<DbhResponse<*>>.item() = this.items().map { it.first() }

    private fun Mono<DbhResponse<*>>.items() =
        this.flatMap {
            when {
                it.isFailure() -> onFailure(it)
                it.isEmpty() -> Mono.empty()
                else -> onSuccess(it)
            }
        }

    private fun onFailure(r: DbhResponse<*>): Mono<List<DatabaseSchemaResource>> =
        SourceSystemException(
            message = "status=${r.status} error=${(r.items.firstOrNull() ?: "") as String}",
            sourceSystem = "dbh"
        ).toMono()

    private fun onSuccess(r: DbhResponse<*>) =
        r.items.map {
            createObjectMapper().convertValue(it, DatabaseSchemaResource::class.java)
        }.filter {
            it.containsRequiredLabels()
        }.toMono()
}

interface DatabaseSchemaService {
    fun getDatabaseSchemas(affiliation: String): List<DatabaseSchemaResource>
    fun getDatabaseSchema(id: String): DatabaseSchemaResource?
    fun updateDatabaseSchema(input: SchemaUpdateRequest): DatabaseSchemaResource
    fun deleteDatabaseSchema(input: SchemaDeletionRequest): Boolean
    fun testJdbcConnection(user: JdbcUser): Boolean
    fun testJdbcConnection(id: String): Boolean
    fun createDatabaseSchema(input: SchemaCreationRequest): DatabaseSchemaResource
}

@Service
@ConditionalOnBean(RequiresDbh::class)
class DatabaseSchemaServiceBlocking(private val databaseSchemaService: DatabaseSchemaServiceReactive) :
    DatabaseSchemaService {

    override fun getDatabaseSchemas(affiliation: String) =
        databaseSchemaService.getDatabaseSchemas(affiliation).blockWithTimeout() ?: emptyList()

    override fun getDatabaseSchema(id: String) =
        databaseSchemaService.getDatabaseSchema(id).blockWithTimeout()

    override fun updateDatabaseSchema(input: SchemaUpdateRequest) =
        databaseSchemaService.updateDatabaseSchema(input).blockNonNullWithTimeout()

    override fun deleteDatabaseSchema(input: SchemaDeletionRequest) =
        databaseSchemaService.deleteDatabaseSchema(input).blockNonNullWithTimeout()

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
class DatabaseSchemaServiceDisabled : DatabaseSchemaService {
    override fun getDatabaseSchemas(affiliation: String) = integrationDisabled()
    override fun getDatabaseSchema(id: String) = integrationDisabled()
    override fun updateDatabaseSchema(input: SchemaUpdateRequest) = integrationDisabled()
    override fun deleteDatabaseSchema(input: SchemaDeletionRequest) = integrationDisabled()
    override fun testJdbcConnection(user: JdbcUser) = integrationDisabled()
    override fun testJdbcConnection(id: String) = integrationDisabled()
    override fun createDatabaseSchema(input: SchemaCreationRequest) = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("DBH integration is disabled for this environment")
}
