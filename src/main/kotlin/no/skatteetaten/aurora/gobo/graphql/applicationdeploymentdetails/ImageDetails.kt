package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.graphql.imagerepository.IsLatestDigestBatchDataLoader
import no.skatteetaten.aurora.gobo.graphql.imagerepository.IsLatestDigestKey
import no.skatteetaten.aurora.gobo.graphql.loadValue
import java.time.Instant
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

data class ImageDetails(
    val imageBuildTime: Instant?,
    val digest: String?,
    val dockerImageTagReference: String?
) {

    fun isLatestDigest(dfe: DataFetchingEnvironment): CompletableFuture<Boolean>? =
        dockerImageTagReference?.let {
            logger.debug("Loading docker image tag reference for tag=$it")
            dfe.loadValue(key = IsLatestDigestKey(digest, ImageTag.fromTagString(it)), loaderClass = IsLatestDigestBatchDataLoader::class)
        }
}
