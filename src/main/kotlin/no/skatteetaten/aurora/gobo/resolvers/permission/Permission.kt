package no.skatteetaten.aurora.gobo.resolvers.permission

data class Permission(val paasPermission: PermissionDetails)

data class PermissionDetails(val view:Boolean, val admin:Boolean)