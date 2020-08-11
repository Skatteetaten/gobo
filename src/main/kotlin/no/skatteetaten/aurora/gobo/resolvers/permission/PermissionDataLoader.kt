package no.skatteetaten.aurora.gobo.resolvers.permission

/*
@Component
class PermissionDataLoader(
    val permissionService: PermissionService
) : KeyDataLoader<Namespace, Permission> {

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
 */
