package no.skatteetaten.aurora.gobo.resolvers

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["graphql.packages=no.skatteetaten.aurora.gobo.resolvers"]
)
abstract class GraphQLTestWithoutDbhAndSkap {
    @LocalServerPort
    private lateinit var port: String

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun initialize() {
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(5)).build()
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
        await().atMost(Duration.ofSeconds(5)).until {
            kotlin.runCatching {
                WebClient
                    .create("http://localhost:$port/graphql")
                    .get()
                    .exchange()
                    .block(Duration.ofSeconds(1))?.let { it.statusCode() == HttpStatus.BAD_REQUEST }
            }.getOrNull() ?: false
        }
    }

    @AfterEach
    fun shutdown() = clearAllMocks()
}

@ActiveProfiles("with-dbh-and-skap")
abstract class GraphQLTestWithDbhAndSkap : GraphQLTestWithoutDbhAndSkap()
