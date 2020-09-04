package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.startsWith
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.StrubrunnerRepoPropertiesEnabler
import no.skatteetaten.aurora.gobo.TestConfig
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
@Disabled
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [TestConfig::class, ApplicationConfig::class, ImageRegistryServiceBlocking::class, SharedSecretReader::class]
)
@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:cantus:+:stubs:6568"])
class ImageRegistryServiceBlockingContractTest : StrubrunnerRepoPropertiesEnabler() {

    @Autowired
    private lateinit var imageRegistry: ImageRegistryServiceBlocking

    private val imageRepoName = "url/namespace/name"
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
            .startsWith("sha")
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
}
