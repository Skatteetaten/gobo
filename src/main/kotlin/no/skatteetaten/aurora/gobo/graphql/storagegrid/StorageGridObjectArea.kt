package no.skatteetaten.aurora.gobo.graphql.storagegrid

import no.skatteetaten.aurora.gobo.integration.mokey.StoragegridObjectAreaResource

data class StorageGridObjectArea(
    val name: String,
    val bucketName: String,
    val namespace: String,
    val creationTimestamp: String,
    val status: StorageGridObjectAreaStatus
) {
    companion object {
        fun fromResource(areaResource: StoragegridObjectAreaResource): StorageGridObjectArea {
            return StorageGridObjectArea(
                name = areaResource.name,
                bucketName = areaResource.bucketName,
                namespace = areaResource.namespace,
                creationTimestamp = areaResource.creationTimestamp,
                status = StorageGridObjectAreaStatus(
                    message = areaResource.message,
                    reason = areaResource.reason,
                    success = areaResource.success
                )
            )
        }
    }
}

data class StorageGridObjectAreaStatus(
    val message: String,
    val reason: String,
    val success: Boolean
)
