package no.skatteetaten.aurora.gobo.integration.cantus

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.integration.SpringTestTag
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import java.time.Instant

@SpringTestTag
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [ApplicationConfig::class, ImageRegistryServiceBlocking::class]
)
@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:cantus:+:stubs:6568"])
class ImageRegistryServiceBlockingContractTest {

    @Autowired
    lateinit var imageRegistry: ImageRegistryServiceBlocking

    private val imageRepoName = "no_skatteetaten_aurora_demo/whoami"
    private val tagName = "2"

    private val imageRepo = ImageRepository.fromRepoString("docker.com/$imageRepoName").toImageRepo()

    private val token: String = "token"

    @Test
    fun `verify fetches all tags for specified repo`() {
        val listOfTags = listOf(
            "SNAPSHOT--dev-20170912.120730-1-b1.4.1-wingnut-8.141.1",
            "dev-SNAPSHOT",
            "latest",
            "1",
            "1.2",
            "1.0.0"
        )

        val expectedTags = TagsDto(
            listOfTags.map {
                Tag(it, ImageTagType.typeOf(it))
            }
        )

        val tags = imageRegistry.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepo, token)
        assertThat(tags).containsAllTags(expectedTags)
    }

    @Test
    fun `verify dockerContentDigest can be found`() {

        val dockerContentDigest = imageRegistry.resolveTagToSha(imageRepo, tagName, token)
        assertThat(dockerContentDigest).isEqualTo("sha256:9d044d853c40b42ba52c576e1d71e5cee7dc4d1b328650e0780cd983cb474ed0")
    }

    @Test
    fun `get tagsByName given repositories and tagNames return AuroraResponse`() {

        val imageReposAndTags = listOf(
            ImageRepoAndTags("docker1.no/no_skatteetaten_aurora_demo/whoami", listOf("2")),
            ImageRepoAndTags("docker2.no/no_skatteetaten_aurora_demo/whoami", listOf("1"))
        )

        val auroraResponse = imageRegistry.findTagsByName(imageReposAndTags, token)
        assertThat(auroraResponse.items.first().timeline.buildEnded).isEqualTo(Instant.parse("2018-11-05T14:01:22.654389192Z"))
    }

    private fun Assert<TagsDto>.containsAllTags(expectedTags: TagsDto) =
        given { tagsDto ->
            if (
                expectedTags.tags.all { expectedTag ->
                    tagsDto.tags.any { actualTag ->
                        actualTag.name == expectedTag.name
                    }
                }) return

            expected("Some tags were not present")
        }
}