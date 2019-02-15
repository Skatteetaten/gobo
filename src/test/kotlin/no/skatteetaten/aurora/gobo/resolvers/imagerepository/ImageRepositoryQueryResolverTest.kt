package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRepoDto
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagDto
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType
import no.skatteetaten.aurora.gobo.integration.imageregistry.Tag
import no.skatteetaten.aurora.gobo.integration.imageregistry.TagsDto
import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrors
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository.Companion.fromRepoString
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
        val imageRepoDto: ImageRepoDto get() = fromRepoString(repoString).toImageRepo("1")
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
            given(imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(data.imageRepoDto)).willReturn(
                TagsDto(data.tags.map {
                    Tag(name = it, type = ImageTagType.typeOf(it))
                })
            )
            data.tags
                .map {
                    ImageTagDto(name = it, created = EPOCH, dockerDigest = "sha256")
                }
                .forEach {
                    given(
                        imageRegistryServiceBlocking.findTagByName(
                            data.imageRepoDto
                        )
                    ).willReturn(it)
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
            .expectBody(QueryResponse.Response::class.java)
            .returnResult().let { result ->
                result.responseBody!!.data.imageRepositories.forEachIndexed { repoIndex, repository ->
                    assertThat(repository.repository).isEqualTo(testData[repoIndex].repoString)
                    assertThat(repository.tags.totalCount).isEqualTo(testData[repoIndex].tags.size)
                    assertThat(repository.tags.edges.size).isEqualTo(testData[repoIndex].tags.size)
                    repository.tags.edges.forEachIndexed { edgeIndex, edge ->
                        assertThat(edge.node.name).isEqualTo(testData[repoIndex].tags[edgeIndex])
                        assertThat(edge.node.lastModified).isEqualTo(Instant.EPOCH.toString())
                    }
                }
            }
    }

    @Test
    fun `Query for tags with paging`() {
        val pageSize = 3
        val variables = mapOf("repositories" to testData[0].imageRepoDto.repository, "pageSize" to pageSize)
        val query = createQuery(tagsWithPagingQuery, variables)

        webTestClient.queryGraphQL(tagsWithPagingQuery, variables)
            .expectStatus().isOk
            .expectBody(QueryResponse.Response::class.java)
            .returnResult().let { result ->
                val tags = result.responseBody!!.data.imageRepositories[0].tags
                assertThat(tags.totalCount).isEqualTo(testData[0].tags.size)
                assertThat(tags.edges.size).isEqualTo(pageSize)
                assertThat(tags.edges.map { it.node.name }).containsExactly("1", "1.0", "1.0.0")
                assertThat(tags.pageInfo?.startCursor).isNotEqualTo("")
                assertThat(tags.pageInfo?.hasNextPage).isNotNull().isTrue()
            }
    }

    @Test
    fun `Get errors when findByTagName fails with exception`() {
        given(imageRegistryServiceBlocking.findTagByName(testData[0].imageRepoDto))
            .willThrow(SourceSystemException("test exception", RuntimeException("testing testing")))

        val variables = mapOf("repositories" to testData[0].imageRepoDto.repository)
        webTestClient.queryGraphQL(reposWithTagsQuery, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlErrors("length()").isEqualTo(1)
            .graphqlErrors("[0].extensions.code").exists()
            .graphqlErrors("[0].extensions.cause").exists()
            .graphqlErrors("[0].extensions.errorMessage").exists()
    }
}

class QueryResponse {
    data class Tag(val name: String, val lastModified: String)
    data class Edge(val node: Tag)
    data class Tags(val totalCount: Int, val edges: List<Edge>, val pageInfo: GoboPageInfo?)
    data class ImageRepository(val repository: String?, val tags: Tags)
    data class ImageRepositories(val imageRepositories: List<ImageRepository>)
    data class Response(val data: ImageRepositories)
}
