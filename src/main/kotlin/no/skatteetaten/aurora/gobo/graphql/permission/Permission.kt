package no.skatteetaten.aurora.gobo.graphql.permission

data class Permission(val paas: PermissionDetails = PermissionDetails())

data class PermissionDetails(val view: Boolean = false, val admin: Boolean = false)
