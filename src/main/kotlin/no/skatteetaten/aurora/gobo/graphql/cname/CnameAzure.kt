package no.skatteetaten.aurora.gobo.graphql.cname

data class CnameAzure(
    val name: String,
    val canonicalName: String,
    val ttlInSeconds: Int,
    val namespace: String,
    val clusterId: String,
    val ownerObjectName: String,
)
