package no.skatteetaten.aurora.gobo.resolvers.permission

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.MyGraphQLContext
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger { }

@Component
class PermissionDataLoader(
    val permissionService: PermissionService
) : KeyDataLoader<Namespace, Permission> {

    override suspend fun getByKey(key: Namespace, ctx: MyGraphQLContext): Permission {
        // FIXME token
        return permissionService.getPermission(key.name, "token").map {
            Permission(paas = PermissionDetails(view = it.view, admin = it.admin))
        }.onErrorResume { err ->
            logger.warn("Failed checking for permission message=${err.localizedMessage}")
            Mono.just(Permission())
        }.blockAndHandleError()!!
    }
}
