package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import no.skatteetaten.aurora.gobo.resolvers.createQuery
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository.Companion.fromRepoString
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRegistryService
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRepo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.Instant
import java.time.Instant.EPOCH

typealias ServiceImageTag = no.skatteetaten.aurora.gobo.service.imageregistry.ImageTag

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["management.server.port=-1"])
@DirtiesContext
class ImageRepositoryQueryResolverTest {
    @Value("classpath:graphql/getImageRepositories.graphql")
    private lateinit var query: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var imageRegistryService: ImageRegistryService

    class ImageRepoData(val repoString: String, val tags: List<String>) {
        val imageRepo: ImageRepo get() = fromRepoString(repoString).toImageRepo()
    }

    @Test
    fun `Query for repositories and tags`() {
        val testData = mapOf(
            "docker-registry.aurora.sits.no:5000/aurora/openshift-jenkins-master" to
                listOf("1", "1.0", "1.0.0", "1.0.1", "latest", "feature_something-SNAPSHOT"),
            "docker-registry.aurora.sits.no:5000/aurora/openshift-jenkins-slave" to
                listOf("2", "2.1", "2.1.3", "latest", "dev-SNAPSHOT")
        ).map { ImageRepoData(it.key, it.value) }

        testData.forEach { data: ImageRepoData ->
            given(imageRegistryService.findTagNamesInRepoOrderedByCreatedDateDesc(data.imageRepo)).willReturn(data.tags)
            data.tags
                .map { ServiceImageTag(it, created = EPOCH) }
                .forEach { given(imageRegistryService.findTagByName(data.imageRepo, it.name)).willReturn(it) }
        }

        val variables = mapOf("repositories" to testData.map { it.imageRepo.repository })
        val query = createQuery(query, variables)

        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.imageRepositories[0].repository").isEqualTo(testData[0].repoString)
            .jsonPath("$.data.imageRepositories[0].tags.totalCount").isEqualTo(testData[0].tags.size)
            .jsonPath("$.data.imageRepositories[0].tags.edges[0].node.name").isEqualTo(testData[0].tags[0])
            .jsonPath("$.data.imageRepositories[0].tags.edges[0].node.lastModified").isEqualTo(Instant.EPOCH.toString())
            .jsonPath("$.data.imageRepositories[1].repository").isEqualTo(testData[1].repoString)
            .jsonPath("$.data.imageRepositories[1].tags.totalCount").isEqualTo(testData[1].tags.size)
    }
}