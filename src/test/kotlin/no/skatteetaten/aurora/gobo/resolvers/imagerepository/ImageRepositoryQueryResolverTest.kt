package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRepoDto
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagDto
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType
import no.skatteetaten.aurora.gobo.integration.imageregistry.Tag
import no.skatteetaten.aurora.gobo.integration.imageregistry.TagsDto
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefixAndIndex
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrors
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository.Companion.fromRepoString
import no.skatteetaten.aurora.gobo.resolvers.isTrue
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import java.time.Instant.EPOCH

@GraphQLTest
class ImageRepositoryQueryResolverTest {
    @Value("classpath:graphql/queries/getImageRepositories.graphql")
    private lateinit var reposWithTagsQuery: Resource

    @Value("classpath:graphql/queries/getImageTagsWithPaging.graphql")
    private lateinit var tagsWithPagingQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var imageRegistryServiceBlocking: ImageRegistryServiceBlocking

    class ImageRepoData(val repoString: String, val tags: List<String>) {
        val imageRepoDto: ImageRepoDto get() = fromRepoString(repoString).toImageRepo()
    }

    val testData = mapOf(
        "docker-registry.aurora.sits.no:5000/aurora/openshift-jenkins-master" to
            listOf("1", "1.0", "1.0.0", "1.0.1", "latest", "feature_something-SNAPSHOT"),
        "docker-registry.aurora.sits.no:5000/aurora/openshift-jenkins-slave" to
            listOf("2", "2.1", "2.1.3", "latest", "dev-SNAPSHOT")
    ).map { ImageRepoData(it.key, it.value) }

    @BeforeEach
    fun setUp() {
        testData.forEach { data: ImageRepoData ->
            given(imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(data.imageRepoDto))
                .willReturn(TagsDto(data.tags.map { Tag(name = it, type = ImageTagType.typeOf(it)) }))
            data.tags
                .map {
                    ImageTagDto(name = it, created = EPOCH, dockerDigest = "sha256")
                }
                .forEach {
                    given(imageRegistryServiceBlocking.findTagByName(data.imageRepoDto, it.name)).willReturn(it)
                }
        }
    }

    @AfterEach
    fun tearDown() = reset(imageRegistryServiceBlocking)

    @Test
    fun `Query for repositories and tags`() {
        val variables = mapOf("repositories" to testData.map { it.imageRepoDto.repository })
        webTestClient.queryGraphQL(reposWithTagsQuery, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefixAndIndex("imageRepositories", endIndex = 1) {
                val repository = testData[it.index]
                it.graphqlData("repository").isEqualTo(repository.repoString)
                it.graphqlData("tags.totalCount").isEqualTo(repository.tags.size)
                it.graphqlData("tags.edges.length()").isEqualTo(repository.tags.size)
                it.graphqlData("tags.edges[0].node.name").isEqualTo(repository.tags[0])
                it.graphqlData("tags.edges[0].node.lastModified").isEqualTo(Instant.EPOCH.toString())
                it.graphqlData("tags.edges[1].node.name").isEqualTo(repository.tags[1])
                it.graphqlData("tags.edges[1].node.lastModified").isEqualTo(Instant.EPOCH.toString())
            }
    }

    @Test
    fun `Query for tags with paging`() {
        val pageSize = 3
        val variables = mapOf("repositories" to testData[0].imageRepoDto.repository, "pageSize" to pageSize)

        webTestClient.queryGraphQL(tagsWithPagingQuery, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("imageRepositories[0].tags") {
                it.graphqlData("totalCount").isEqualTo(testData[0].tags.size)
                it.graphqlData("edges.length()").isEqualTo(pageSize)
                it.graphqlData("edges[0].node.name").isEqualTo("1")
                it.graphqlData("edges[1].node.name").isEqualTo("1.0")
                it.graphqlData("edges[2].node.name").isEqualTo("1.0.0")
                it.graphqlData("pageInfo.startCursor").isNotEmpty
                it.graphqlData("pageInfo.hasNextPage").isTrue()
            }
    }

    @Test
    fun `Get errors when findByTagName fails with exception`() {
        given(imageRegistryServiceBlocking.findTagByName(testData[0].imageRepoDto, testData[0].tags[0]))
            .willThrow(SourceSystemException("test exception", RuntimeException("testing testing")))

        val variables = mapOf("repositories" to testData[0].imageRepoDto.repository)
        webTestClient.queryGraphQL(reposWithTagsQuery, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlErrors("length()").isEqualTo(1)
            .graphqlErrorsFirst("extensions.code").exists()
            .graphqlErrorsFirst("extensions.cause").exists()
            .graphqlErrorsFirst("extensions.errorMessage").exists()
    }
}
