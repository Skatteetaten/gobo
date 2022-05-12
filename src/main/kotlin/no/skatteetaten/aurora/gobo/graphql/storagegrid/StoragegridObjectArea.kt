package no.skatteetaten.aurora.gobo.graphql.storagegrid

import no.skatteetaten.aurora.gobo.integration.mokey.StoragegridObjectAreaResource

data class StoragegridObjectArea(
    val name: String,
    val bucketName: String,
    val namespace: String,
    val creationTimestamp: String,
    val status: StoragegridObjectAreaStatus
) {
    companion object {
        fun fromResource(areaResource: StoragegridObjectAreaResource): StoragegridObjectArea {
            return StoragegridObjectArea(
                name = areaResource.name,
                bucketName = areaResource.bucketName,
                namespace = areaResource.namespace,
                creationTimestamp = areaResource.creationTimestamp,
                status = StoragegridObjectAreaStatus(
                    message = areaResource.message,
                    reason = areaResource.reason,
                    success = areaResource.success
                )
            )
        }
    }
}

data class StoragegridObjectAreaStatus(
    val message: String,
    val reason: String,
    val success: Boolean
)
