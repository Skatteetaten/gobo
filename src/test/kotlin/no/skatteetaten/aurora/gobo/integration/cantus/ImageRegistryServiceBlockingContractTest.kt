package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.startsWith
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.integration.SpringTestTag
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner

@SpringTestTag
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [ApplicationConfig::class, ImageRegistryServiceBlocking::class]
)
@AutoConfigureStubRunner(
    ids = ["no.skatteetaten.aurora:cantus:AOS_3290_post_for_queryparametre-SNAPSHOT:stubs:6568"],
    repositoryRoot = "http://aurora/nexus/content/repositories/snapshots"
)
@Disabled
class ImageRegistryServiceBlockingContractTest {

    @Autowired
    private lateinit var imageRegistry: ImageRegistryServiceBlocking

    private val imageRepoName = "docker1.no/no_skatteetaten_aurora_demo/whoami"
    private val tagName = "1"

    private val imageRepo = ImageRepository.fromRepoString(imageRepoName).toImageRepo()

    private val token: String = "token"

    @Test
    fun `verify fetches all tags for specified repo`() {

        val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo, token)
        assertThat(tags.tags).isNotEmpty()
    }

    @Test
    fun `verify dockerContentDigest can be found`() {

        val dockerContentDigest = imageRegistry.resolveTagToSha(imageRepo, tagName, token)

        assertThat(dockerContentDigest)
            .isNotNull()
            .startsWith("sha256:")
    }

    @Test
    fun `getTagsByName given repositories and tagNames return AuroraResponse`() {

        val imageReposAndTags = listOf(
            ImageRepoAndTags("docker1.no/no_skatteetaten_aurora_demo/whoami", listOf("1")),
            ImageRepoAndTags("docker2.no/no_skatteetaten_aurora_demo/whoami", listOf("2"))
        )

        val auroraResponse = imageRegistry.findTagsByName(imageReposAndTags, token)
        assertThat(auroraResponse.items.forEach { it.timeline.buildEnded != null })
        assertThat(auroraResponse.failure.forEach { it.errorMessage.isNotEmpty() })
    }

    @Ignore
    @Test
    fun `getTagsByName given non existing tag for image return AuroraResponse with CantusFailure`() {
        val repository = "docker1.no/no_skatteetaten_aurora_demo/whoami"
        val tag = "10"
        val imageRepoAndTags =
            listOf(ImageRepoAndTags(repository, listOf(tag)))

        val auroraResponseFailure = imageRegistry.findTagsByName(imageRepoAndTags, token)
        assertThat(auroraResponseFailure.failureCount).isGreaterThan(0)
        assertThat(auroraResponseFailure.failure.all { it.url.isNotEmpty() })
        assertThat(auroraResponseFailure.failure.any { it.errorMessage.endsWith("status=404 message=Not Found") })
    }
}