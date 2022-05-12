package no.skatteetaten.aurora.gobo.graphql.credentials

import com.expediagroup.graphql.server.operations.Mutation
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
import no.skatteetaten.aurora.gobo.graphql.storagegrid.getTenantName

private val logger = KotlinLogging.logger {}

@ConditionalOnProperty("integrations.storagegrid.operator.application.deployment.id")
@Component
class StorageGridCredentialMutation(
    private val herkimerService: HerkimerService,
    private val nagHubService: NagHubService,
    @Value("\${integrations.storagegrid.operator.application.deployment.id}") val storagegridOperatorAdId: String,
    @Value("\${openshift.cluster}") val cluster: String,
    @Value("\${credentials.registerStorageGrid.notificationChannel}") val notificationChannel: String,
    @Value("\${credentials.registerStorageGrid.allowedAdGroup}") val allowedAdGroup: String
) : Mutation {
    suspend fun registerStorageGridTenant(
        input: StorageGridTenantInput,
        dfe: DataFetchingEnvironment
    ): RegisterStorageGridTenantResult {
        dfe.checkIsUserAuthorized(allowedAdGroup)

        val tenantName = getTenantName(input.businessGroup, cluster)

        return herkimerService.registerResourceAndClaim(
            createRegisterResourceAndClaimCommand(input, tenantName)
        ).toRegisterStorageGridTenantResult(tenantName)
            .also {
                if (notificationChannel.isNotEmpty())
                    nagHubService.sendMessage(notificationChannel, it.toNotifyMessage(tenantName))
                        ?.logOnFailure(tenantName)
            }
    }

    private fun createRegisterResourceAndClaimCommand(input: StorageGridTenantInput, tenantName: String): RegisterResourceAndClaimCommand {
        return RegisterResourceAndClaimCommand(
            ownerId = storagegridOperatorAdId,
            credentials = StorageGridTenantAdminCredentials(
                accountId = input.accountId,
                username = input.username,
                password = input.password
            ),
            resourceName = tenantName,
            claimName = "ADMIN",
            resourceKind = ResourceKind.StorageGridTenant
        )
    }

    private fun RegisterStorageGridTenantResult.toNotifyMessage(tenantName: String): List<DetailedMessage> {
        val notifyMessage =
            if (success) {
                DetailedMessage(
                    NagHubColor.Yellow, "$message."
                )
            } else {
                DetailedMessage(
                    NagHubColor.Red,
                    "$message. The tenant account needs to be manually registered for tenantName=$tenantName."
                )
            }

        return listOf(notifyMessage)
    }

    private fun NagHubResult.logOnFailure(tenantName: String) {
        if (!success) {
            logger.error {
                "Could not send notification to mattermost for a registered StorageGrid tenant tenantName=$tenantName"
            }
        }
    }
}

private fun HerkimerResult.toRegisterStorageGridTenantResult(tenantName: String): RegisterStorageGridTenantResult {
    val message =
        if (success) "StorageGrid tenant tenantName=$tenantName has been successfully registered in the Aurora Platform."
        else "StorageGrid tenant tenantName=$tenantName could not be registered. The Aurora Platform has internal configuration issues."

    return RegisterStorageGridTenantResult(
        message,
        this.success
    )
}

data class StorageGridTenantInput(
    val accountId: String,
    val username: String,
    val password: String,
    val businessGroup: String
)

data class RegisterStorageGridTenantResult(
    val message: String,
    val success: Boolean
)

data class StorageGridTenantAdminCredentials(
    val accountId: String,
    val username: String,
    val password: String,
) : CredentialBase
