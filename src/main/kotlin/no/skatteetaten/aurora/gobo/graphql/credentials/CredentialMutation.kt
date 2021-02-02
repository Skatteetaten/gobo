package no.skatteetaten.aurora.gobo.graphql.credentials

import com.expediagroup.graphql.spring.operations.Mutation
import com.fasterxml.jackson.annotation.JsonProperty
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.AccessDeniedException
import no.skatteetaten.aurora.gobo.integration.herkimer.CredentialBase
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerResult
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import no.skatteetaten.aurora.gobo.integration.herkimer.RegisterResourceAndClaimCommand
import no.skatteetaten.aurora.gobo.integration.herkimer.ResourceKind
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class CredentialMutation(
    private val herkimerService: HerkimerService,
    @Value("\${integrations.dbh.application.deployment.id}") val dbhAdId: String
) : Mutation {
    suspend fun registerPostgresMotelServer(
        input: PostgresMotelInput,
        dfe: DataFetchingEnvironment
    ): RegisterPostgresResult {
        dfe.checkIsUserAuthorized()

        val postgresInstance = input.toHerkimerPostgresInstance()

        return herkimerService.registerResourceAndClaim(
            RegisterResourceAndClaimCommand(
                ownerId = dbhAdId,
                credentials = postgresInstance,
                resourceName = postgresInstance.instanceName,
                claimName = "ADMIN",
                resourceKind = ResourceKind.PostgresDatabaseInstance
            )
        ).toRegisterPostgresResult(input.host)
    }
}
private suspend fun DataFetchingEnvironment.checkIsUserAuthorized() {
    val userId = this.checkValidUserToken().id

    if (!userId.matches(Regex("system:serviceaccount:aurora[-\\w]*:vra$"))) {
        throw AccessDeniedException("You do not have access to register a Postgres Motel Server")
    }
}

private fun HerkimerResult.toRegisterPostgresResult(host: String): RegisterPostgresResult {
    val message =
        if (!this.success) "PostgresMotel host=$host could not be registered. The AuroraPlattform has internal configuration issues."
        else "PostgresMotel host=$host has been successfully added."

    return RegisterPostgresResult(
        message,
        this.success
    )
}

private fun PostgresMotelInput.toHerkimerPostgresInstance() =
    PostgresHerkimerDatabaseInstance(
        host = host,
        port = 5432,
        // TODO: Should instanceName be a part of the input or should we deduce it?
        instanceName = "${host.substringBefore("-")}-postgres",
        username = username,
        password = password,
        affiliation = businessGroup
    )

data class RegisterPostgresResult(
    val message: String,
    val success: Boolean
)

data class PostgresMotelInput(
    val host: String,
    val username: String,
    val password: String,
    val businessGroup: String
)

data class PostgresHerkimerDatabaseInstance(
    val host: String,
    val port: Int,
    val instanceName: String,
    val username: String,
    val password: String,
    @JsonProperty("labels_affiliation")
    val affiliation: String,
    val engine: String = "postgres"
) : CredentialBase
