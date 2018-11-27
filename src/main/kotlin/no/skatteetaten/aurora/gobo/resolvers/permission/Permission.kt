package no.skatteetaten.aurora.gobo.resolvers.permission

data class Permission(val paas: PermissionDetails)

data class PermissionDetails(val view: Boolean, val admin: Boolean)