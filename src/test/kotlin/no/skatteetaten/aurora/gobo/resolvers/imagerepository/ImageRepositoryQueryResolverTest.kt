package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import assertk.assert
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository.Companion.fromRepoString
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRegistryService
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRegistryServiceErrorException
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRepo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.Instant
import java.time.Instant.EPOCH

typealias ServiceImageTag = no.skatteetaten.aurora.gobo.service.imageregistry.ImageTag

@GraphQLTest
class ImageRepositoryQueryResolverTest {
    @Value("classpath:graphql/getImageRepositories.graphql")
    private lateinit var reposWithTagsQuery: Resource

    @Value("classpath:graphql/getImageTagsWithPaging.graphql")
    private lateinit var tagsWithPagingQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var imageRegistryService: ImageRegistryService

    class ImageRepoData(val repoString: String, val tags: List<String>) {
        val imageRepo: ImageRepo get() = fromRepoString(repoString).toImageRepo()
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
            given(imageRegistryService.findTagNamesInRepoOrderedByCreatedDateDesc(data.imageRepo)).willReturn(data.tags)
            data.tags
                    .map { ServiceImageTag(it, created = EPOCH) }
                    .forEach { given(imageRegistryService.findTagByName(data.imageRepo, it.name)).willReturn(it) }
        }
    }

    @Test
    fun `Query for repositories and tags`() {
        val variables = mapOf("repositories" to testData.map { it.imageRepo.repository })
        val query = createQuery(reposWithTagsQuery, variables)

        webTestClient
                .post()
                .uri("/graphql")
                .body(BodyInserters.fromObject(query))
                .exchange()
                .expectStatus().isOk
                .expectBody(QueryResponse.Response::class.java)
                .consumeWith<Nothing> { result ->
                    result.responseBody!!.data.imageRepositories.forEachIndexed { repoIndex, repository ->
                        assert(repository.repository).isEqualTo(testData[repoIndex].repoString)
                        assert(repository.tags.totalCount).isEqualTo(testData[repoIndex].tags.size)
                        assert(repository.tags.edges.size).isEqualTo(testData[repoIndex].tags.size)
                        repository.tags.edges.forEachIndexed { edgeIndex, edge ->
                            assert(edge.node.name).isEqualTo(testData[repoIndex].tags[edgeIndex])
                            assert(edge.node.lastModified).isEqualTo(Instant.EPOCH.toString())
                        }
                    }
                }
    }

    @Test
    fun `Query for tags with paging`() {
        val pageSize = 3
        val variables = mapOf("repositories" to testData[0].imageRepo.repository, "pageSize" to pageSize)
        val query = createQuery(tagsWithPagingQuery, variables)

        webTestClient
                .post()
                .uri("/graphql")
                .body(BodyInserters.fromObject(query))
                .exchange()
                .expectStatus().isOk
                .expectBody(QueryResponse.Response::class.java)
                .consumeWith<Nothing> { result ->
                    val tags = result.responseBody!!.data.imageRepositories[0].tags
                    assert(tags.totalCount).isEqualTo(testData[0].tags.size)
                    assert(tags.edges.size).isEqualTo(pageSize)
                    assert(tags.edges.map { it.node.name }).containsExactly("1", "1.0", "1.0.0")
                    assert(tags.pageInfo!!.startCursor).isNotEmpty()
                    assert(tags.pageInfo.hasNextPage).isTrue()
                }
    }

    @Test
    fun `Get errors when findByTagName fails with exception`() {
        given(imageRegistryService.findTagByName(testData[0].imageRepo, testData[0].tags[0]))
                .willThrow(ImageRegistryServiceErrorException("test exception"))

        val variables = mapOf("repositories" to testData[0].imageRepo.repository)
        val query = createQuery(reposWithTagsQuery, variables)
        webTestClient
                .post()
                .uri("/graphql")
                .body(BodyInserters.fromObject(query))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.errors.length()").isEqualTo(1)
                .jsonPath("$.errors[0].extensions.code").exists()
                .jsonPath("$.errors[0].extensions.cause").exists()
                .jsonPath("$.errors[0].extensions.errorMessage").exists()
    }
}

class QueryResponse {
    data class PageInfo(val startCursor: String, val hasNextPage: Boolean)
    data class Tag(val name: String, val lastModified: String)
    data class Edge(val node: Tag)
    data class Tags(val totalCount: Int, val edges: List<Edge>, val pageInfo: PageInfo?)
    data class ImageRepository(val repository: String?, val tags: Tags)
    data class ImageRepositories(val imageRepositories: List<ImageRepository>)
    data class Response(val data: ImageRepositories)
}
