package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.CantusFailure
import no.skatteetaten.aurora.gobo.integration.cantus.ImageBuildTimeline
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoAndTags
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRepoDto
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagResource
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrors
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
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

private fun ImageRepoAndTags.copyImageTagSublist(toIndex: Int) =
    this.copy(imageTags = this.imageTags.subList(0, toIndex))

@GraphQLTest
class ImageRepositoryQueryResolverTest {
    @Value("classpath:graphql/queries/getImageRepositories.graphql")
    private lateinit var reposWithTagsQuery: Resource

    @Value("classpath:graphql/queries/getImageTagsWithPaging.graphql")
    private lateinit var tagsWithPagingQuery: Resource

    @Value("classpath:graphql/queries/getImageRepositoriesWithNoTagFiltering.graphql")
    private lateinit var reposWithTagsWithoutFiltersQuery: Resource

    @Value("classpath:graphql/queries/getImageRepositoriesWithOnlyFirstFilter.graphql")
    private lateinit var reposWithOnlyFirstFilter: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var imageRegistryServiceBlocking: ImageRegistryServiceBlocking

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

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
            given(
                imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(
                    ImageRepoDto.fromRepoString(imageRepoAndTags.imageRepository),
                    "test-token"
                )
            ).willReturn(TagsDto(imageRepoAndTags.imageTags.map { Tag(name = it, type = ImageTagType.typeOf(it)) }))
        }

        given(openShiftUserLoader.findOpenShiftUserByToken(BDDMockito.anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(imageRegistryServiceBlocking, openShiftUserLoader)

    /*
    @Test
    fun `Query for repositories and tags`() {

        given(imageRegistryServiceBlocking.findTagsByName(imageReposAndTags, "test-token")).willReturn(auroraResponse)

        val variables = mapOf("repositories" to imageReposAndTags.map { it.imageRepository })
        webTestClient.queryGraphQL(reposWithTagsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefixAndIndex("imageRepositories", endIndex = 1) {
                val imageRepoAndTags = imageReposAndTags[index]

                graphqlData("repository").isEqualTo(imageRepoAndTags.imageRepository)
                graphqlData("tags.totalCount").isEqualTo(imageRepoAndTags.imageTags.size)
                graphqlData("tags.edges.length()").isEqualTo(imageRepoAndTags.imageTags.size)
                graphqlData("tags.edges[0].node.name").isEqualTo(imageRepoAndTags.imageTags[0])
                graphqlData("tags.edges[0].node.image.buildTime").isEqualTo(Instant.EPOCH.toString())
                graphqlData("tags.edges[1].node.name").isEqualTo(imageRepoAndTags.imageTags[1])
                graphqlData("tags.edges[1].node.image.buildTime").isEqualTo(Instant.EPOCH.toString())
            }
    }

    @Test
    fun `Query for repositories with empty array input`() {
        webTestClient.queryGraphQL(
            queryResource = reposWithTagsQuery,
            variables = mapOf("repositories" to listOf<String>()),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirst("message").isEqualTo("repositories is empty")
    }
     */

    @Test
    fun `Query for tags with no filters present`() {
        webTestClient.queryGraphQL(
            queryResource = reposWithTagsWithoutFiltersQuery,
            variables = mapOf("repositories" to imageReposAndTags.first().imageRepository),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirst("message")
            .isEqualTo("Validation error of type MissingFieldArgument: Missing field argument types @ 'imageRepositories/tags'")
    }

    @Test
    fun `Query for tags with only first filter present`() {
        webTestClient.queryGraphQL(
            queryResource = reposWithOnlyFirstFilter,
            variables = mapOf("repositories" to imageReposAndTags.first().imageRepository),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirst("message")
            .isEqualTo("Validation error of type MissingFieldArgument: Missing field argument types @ 'imageRepositories/tags'")
    }

    /*
    @Test
    fun `Query for tags with paging`() {
        val pageSize = 3
        val variables = mapOf("repositories" to imageReposAndTags.first().imageRepository, "pageSize" to pageSize)

        given(imageRegistryServiceBlocking.findTagsByName(any(), any())).willReturn(createAuroraResponse(pageSize))

        webTestClient.queryGraphQL(tagsWithPagingQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("imageRepositories[0].tags") {
                graphqlData("totalCount").isEqualTo(imageReposAndTags.first().imageTags.size)
                graphqlData("edges.length()").isEqualTo(pageSize)
                graphqlData("edges[0].node.name").isEqualTo("1")
                graphqlData("edges[1].node.name").isEqualTo("1.0")
                graphqlData("edges[2].node.name").isEqualTo("1.0.0")
                graphqlData("pageInfo.startCursor").isNotEmpty
                graphqlData("pageInfo.hasNextPage").isTrue()
            }
    }

    @Test
    fun `Get errors when findTagsByName fails with exception`() {
        given(
            imageRegistryServiceBlocking.findTagsByName(
                listOf(imageReposAndTags.first()),
                "test-token"
            )
        )
            .willThrow(SourceSystemException("test exception", RuntimeException("testing testing")))

        val variables = mapOf("repositories" to imageReposAndTags.first().imageRepository)
        webTestClient.queryGraphQL(reposWithTagsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlErrors("length()").isEqualTo(6)
            .graphqlErrorsFirst("extensions.code").exists()
            .graphqlErrorsFirst("extensions.cause").exists()
            .graphqlErrorsFirst("extensions.errorMessage").exists()

    }
     */

    @Disabled("partial results within same data loader used to work, but not any more. Bug in graphql library?")
    @Test
    fun `Get items and failures when findTagsByName return partial result`() {
        reset(imageRegistryServiceBlocking)
        val firstImageRepoAndTags = imageReposAndTags.first()

        val tagsDto = TagsDto(
            listOf(
                Tag(firstImageRepoAndTags.imageTags[0]),
                Tag(firstImageRepoAndTags.imageTags[1])
            )
        )

        given(
            imageRegistryServiceBlocking
                .findTagNamesInRepoOrderedByCreatedDateDesc(
                    imageRepoDto = ImageRepoDto.fromRepoString(firstImageRepoAndTags.imageRepository),
                    token = "test-token"
                )
        ).willReturn(tagsDto)

        val partialAuroraResponse = createAuroraResponse(0, 1)
        val imageReposAndTags1 = listOf(firstImageRepoAndTags.copyImageTagSublist(2))
        given(imageRegistryServiceBlocking.findTagsByName(imageReposAndTags1, "test-token")).willReturn(
            partialAuroraResponse
        )

        val variables = mapOf("repositories" to firstImageRepoAndTags.imageRepository)
        webTestClient.queryGraphQL(reposWithTagsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("imageRepositories[0]") {
                val repository = imageReposAndTags.first()

                graphqlData("repository").isEqualTo(repository.imageRepository)
                graphqlData("tags.totalCount").isEqualTo(2)
            }
            .graphqlErrors("length()").isEqualTo(1)
            .graphqlErrorsFirst("extensions.errorMessage").exists()
    }

    private fun createAuroraResponse(itemsCount: Int = imageReposAndTags.getTagCount()): AuroraResponse<ImageTagResource> {
        val imageTagResources = imageReposAndTags.flatMap { it.toImageTagResource() }.subList(0, itemsCount)

        return AuroraResponse(items = imageTagResources)
    }

    private fun createAuroraResponse(successIndex: Int, failureIndex: Int): AuroraResponse<ImageTagResource> {
        val successResource = listOf(imageReposAndTags.first().toImageTagResource()[successIndex])
        val failureResource =
            listOf(
                CantusFailure(
                    url = imageReposAndTags.first().getTagUrls()[failureIndex],
                    errorMessage = "Error response"
                )
            )

        return AuroraResponse(items = successResource, failure = failureResource)
    }
}
