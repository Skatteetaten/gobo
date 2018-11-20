package no.skatteetaten.aurora.gobo.resolvers.permission

import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class PermissionDataLoader(
    val permissionService: PermissionService
) : KeyDataLoader<Namespace, Permission> {
    override fun getByKey(user: User, key: Namespace): Try<Permission> {
        return Try.tryCall {
            permissionService.getPermission(key.name).map {
                Permission(paas = PermissionDetails(view = it.view, admin = it.admin))
            }.blockAndHandleError()
        }
    }
}