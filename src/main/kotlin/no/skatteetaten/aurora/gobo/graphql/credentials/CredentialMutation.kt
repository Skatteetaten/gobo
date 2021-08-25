package no.skatteetaten.aurora.gobo.graphql.credentials

import com.expediagroup.graphql.server.operations.Mutation
import com.fasterxml.jackson.annotation.JsonProperty
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.herkimer.CredentialBase
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerResult
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import no.skatteetaten.aurora.gobo.integration.herkimer.RegisterResourceAndClaimCommand
import no.skatteetaten.aurora.gobo.integration.herkimer.ResourceKind
import no.skatteetaten.aurora.gobo.integration.naghub.DetailedMessage
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubColor
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubResult
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubService
import no.skatteetaten.aurora.gobo.security.checkIsUserAuthorized
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@ConditionalOnProperty("integrations.dbh.application.deployment.id")
@Component
class CredentialMutation(
    private val herkimerService: HerkimerService,
    private val nagHubService: NagHubService,
    @Value("\${integrations.dbh.application.deployment.id}") val dbhAdId: String,
    @Value("\${openshift.cluster}") val cluster: String,
    @Value("\${credentials.registerPostgres.notificationChannel}") val notificationChannel: String,
    @Value("\${credentials.registerPostgres.allowedAdGroup}") val allowedAdGroup: String

) : Mutation {
    suspend fun registerPostgresMotelServer(
        input: PostgresMotelInput,
        dfe: DataFetchingEnvironment
    ): RegisterPostgresResult {
        dfe.checkIsUserAuthorized(allowedAdGroup)

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
            .also {
                nagHubService.sendMessage(notificationChannel, it.toNotifyMessage())
                    ?.logOnFailure(input.host)
            }
    }

    private fun NagHubResult.logOnFailure(host: String) {
        if (!success) {
            logger.error {
                "Could not send notification to mattermost for a registered postgres motel host=$host cluster=$cluster"
            }
        }
    }

    private fun RegisterPostgresResult.toNotifyMessage(): List<DetailedMessage> {
        val notifyMessage =
            if (success) {
                DetailedMessage(
                    NagHubColor.Yellow,
                    "$message. DBH needs to be redeployed in cluster=$cluster"
                )
            } else {
                DetailedMessage(
                    NagHubColor.Red,
                    "$message. The host needs to be manually registered for cluster=$cluster."
                )
            }

        return listOf(notifyMessage)
    }
}

private fun PostgresMotelInput.generateInstanceName(): String {
    val numbering = host.substringAfter("pgsql")
    return "$businessGroup-postgres-$numbering"
}

private fun PostgresMotelInput.toHerkimerPostgresInstance() =
    PostgresHerkimerDatabaseInstance(
        host = host,
        port = 5432,
        username = username,
        password = password,
        affiliation = businessGroup,
        instanceName = generateInstanceName()
    )

private fun HerkimerResult.toRegisterPostgresResult(host: String): RegisterPostgresResult {
    val message =
        if (success) "PostgresMotel host=$host has been successfully registered in the AuroraPlattform."
        else "PostgresMotel host=$host could not be registered. The AuroraPlattform has internal configuration issues."

    return RegisterPostgresResult(
        message,
        this.success
    )
}

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
    val instanceName: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    @JsonProperty("labels_affiliation")
    val affiliation: String,
    val engine: String = "postgres"
) : CredentialBase
