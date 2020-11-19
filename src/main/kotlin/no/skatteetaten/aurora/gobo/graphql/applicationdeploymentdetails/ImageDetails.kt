package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagDto
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.graphql.loadOrThrow
import java.time.Instant

private val logger = KotlinLogging.logger {}

data class ImageDetails(
    val imageBuildTime: Instant?,
    val digest: String?,
    val dockerImageTagReference: String?
) {

    suspend fun isLatestDigest(dfe: DataFetchingEnvironment): Boolean? =
        dockerImageTagReference?.let {
            logger.debug("Loading docker image tag reference for tag=$it")
            val imageTagDto: ImageTagDto = dfe.loadOrThrow(ImageTag.fromTagString(it))
            imageTagDto.dockerDigest == digest
        }
}
