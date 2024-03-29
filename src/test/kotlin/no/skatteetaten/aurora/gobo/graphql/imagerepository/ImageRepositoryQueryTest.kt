package no.skatteetaten.aurora.gobo.graphql.imagerepository

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.AuroraIntegration
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefixAndIndex
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.ImageBuildTimeline
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagResource
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import no.skatteetaten.aurora.gobo.integration.cantus.Version
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import java.time.Instant.EPOCH

private fun ImageRepoAndTags.toImageTagResource() =
    this.imageTags.map {
        ImageTagResource(
            requestUrl = "${this.imageRepository}/$it",
            dockerDigest = "sha256",
            dockerVersion = "2",
            timeline = ImageBuildTimeline(null, EPOCH)
        )
    }

private fun List<ImageRepoAndTags>.getTagCount() =
    this.flatMap { it.imageTags }.size

@Import(
    ImageRepositoryQuery::class,
    VersionsDataLoader::class,
    VersionDataLoader::class,
    GuiUrlDataLoader::class,
    MultipleImagesDataLoader::class
)
class ImageRepositoryQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getVersions.graphql")
    private lateinit var reposWithVersionsQuery: Resource

    @Value("classpath:graphql/queries/getImageTag.graphql")
    private lateinit var imageTagQuery: Resource

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryService

    @MockkBean(relaxed = true)
    private lateinit var auroraIntegration: AuroraIntegration

    private val imageReposAndTags = listOf(
        ImageRepoAndTags(
            imageRepository = "docker-registry.aurora.sits.no:5000/aurora/openshift-jenkins-master",
            imageTags = listOf("1", "1.0", "1.0.0", "1.0.1", "latest", "feature_something-SNAPSHOT")
        ),
        ImageRepoAndTags(
            imageRepository = "docker-registry.aurora.sits.no:5000/aurora/openshift-jenkins-slave",
            imageTags = listOf("2", "2.1", "2.1.3", "latest", "dev-SNAPSHOT")
        )
    )

    private val auroraResponse = createAuroraResponse()

    @BeforeEach
    fun setUp() {
        imageReposAndTags.forEach { imageRepoAndTags ->
            coEvery {
                imageRegistryService.findTagNamesInRepoOrderedByCreatedDateDesc(
                    ImageRepository.fromRepoString(imageRepoAndTags.imageRepository).toImageRepo(),
                    "test-token"
                )
            } returns TagsDto(imageRepoAndTags.imageTags.map { Tag(name = it, type = ImageTagType.typeOf(it)) })

            val imageRepository = ImageRepository.fromRepoString(imageRepoAndTags.imageRepository)

            coEvery {
                imageRegistryService.findVersions(
                    imageRepository.namespace,
                    imageRepository.name,
                    any()
                )
            } returns imageRepoAndTags.imageTags.map { Version(it, EPOCH.toString()) }
        }
    }

    @Test
    fun `Query for tag`() {
        val repo = "docker-registry.aurora.sits.no:5000/aurora/openshift-jenkins-master"
        val imageTags = listOf("latest", "1")
        val query = listOf(
            ImageRepoAndTags(
                imageRepository = repo,
                imageTags = imageTags
            )
        )

        coEvery { imageRegistryService.findTagsByName(any(), any()) } returns auroraResponse

        val variables =
            mapOf("repositories" to query.map { it.imageRepository }, "tagNames" to query.flatMap { it.imageTags })
        webTestClient.queryGraphQL(imageTagQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("imageRepositories[0]") {
                graphqlData("repository").isEqualTo(repo)
                graphqlData("tag[0].name").isEqualTo("latest")
                graphqlData("tag[0].type").isEqualTo("LATEST")
                graphqlData("tag[0].image.buildTime").isEqualTo(EPOCH.toString())
                graphqlData("tag[1].name").isEqualTo("1")
                graphqlData("tag[1].type").isEqualTo("MAJOR")
                graphqlData("tag[1].image.buildTime").isEqualTo(EPOCH.toString())
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for versions`() {

        imageReposAndTags.forEach { imageRepoAndTags ->

            val imageRepository = ImageRepository.fromRepoString(imageRepoAndTags.imageRepository)

            coEvery {
                imageRegistryService.findVersions(
                    imageRepository.namespace,
                    imageRepository.name,
                    any()
                )
            } returns imageRepoAndTags.imageTags.map { Version(it, EPOCH.toString()) }

            coEvery {
                imageRegistryService.findTagNamesInRepoOrderedByCreatedDateDesc(
                    imageRepository.toImageRepo(),
                    "test-token"
                )
            } returns TagsDto(imageRepoAndTags.imageTags.map { Tag(name = it, type = ImageTagType.typeOf(it)) })
        }

        val variables = mapOf("repositories" to imageReposAndTags.map { it.imageRepository })

        webTestClient
            .queryGraphQL(reposWithVersionsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefixAndIndex("imageRepositories", endIndex = 1) {
                val imageRepoAndTags = imageReposAndTags[index]
                graphqlData("repository").isEqualTo(imageRepoAndTags.imageRepository)
                graphqlData("versions.length()").isEqualTo(imageRepoAndTags.imageTags.size)
                graphqlData("versions[0].name").isEqualTo(imageRepoAndTags.imageTags[0])
                graphqlData("versions[0].version.buildTime").isEqualTo(EPOCH.toString())
                graphqlData("versions[1].name").isEqualTo(imageRepoAndTags.imageTags[1])
                graphqlData("versions[1].version.buildTime").isEqualTo(EPOCH.toString())
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get errors when findVersions fails with exception`() {

        coEvery {
            imageRegistryService.findVersions(any(), any(), "test-token")
        } throws SourceSystemException("test exception", RuntimeException("testing testing"))

        val variables = mapOf("repositories" to imageReposAndTags.first().imageRepository)

        webTestClient
            .queryGraphQL(reposWithVersionsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlErrors("length()").isEqualTo(6)
            .graphqlErrorsFirst("extensions.errorMessage").exists()
    }

    private fun createAuroraResponse(itemsCount: Int = imageReposAndTags.getTagCount()): AuroraResponse<ImageTagResource> {
        val imageTagResources = imageReposAndTags.flatMap { it.toImageTagResource() }.subList(0, itemsCount)

        return AuroraResponse(items = imageTagResources)
    }
}
