package no.skatteetaten.aurora.gobo.resolvers.permission

import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PermissionDataLoader(
    val permissionService: PermissionService
) : KeyDataLoader<Namespace, Permission> {

    private val logger = LoggerFactory.getLogger(PermissionDataLoader::class.java)

    override fun getByKey(user: User, key: Namespace): Try<Permission> {
        return Try.tryCall {
            permissionService.getPermission(key.name, user.token).map {
                Permission(paas = PermissionDetails(view = it.view, admin = it.admin))
            }.onErrorResume { err ->
                logger.warn("Failed checking for permission message=${err.localizedMessage}")
                Mono.just(Permission())
            }.blockAndHandleError()
        }
    }
}