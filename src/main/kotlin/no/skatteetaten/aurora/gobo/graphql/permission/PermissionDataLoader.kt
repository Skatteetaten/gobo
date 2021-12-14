package no.skatteetaten.aurora.gobo.graphql.permission

import graphql.GraphQLContext
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.klientid
import no.skatteetaten.aurora.gobo.graphql.korrelasjonsid
import no.skatteetaten.aurora.gobo.graphql.namespace.Namespace
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class PermissionDataLoader(private val permissionService: PermissionService) : GoboDataLoader<Namespace, Permission>() {
    override suspend fun getByKeys(keys: Set<Namespace>, ctx: GraphQLContext): Map<Namespace, Permission> {
        return keys.associateWith {
            runCatching {
                permissionService.getPermission(it.name, ctx.token).let {
                    Permission(paas = PermissionDetails(view = it.view, admin = it.admin))
                }
            }.recoverCatching {
                logger.warn("Failed checking for permission message=${it.localizedMessage}, Korrelasjonsid=${ctx.korrelasjonsid} Klientid=${ctx.klientid}")
                Permission()
            }.getOrThrow()
        }
    }
}
