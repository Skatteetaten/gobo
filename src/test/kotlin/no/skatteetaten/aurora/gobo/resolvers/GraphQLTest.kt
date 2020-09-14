package no.skatteetaten.aurora.gobo.resolvers

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["graphql.packages=no.skatteetaten.aurora.gobo.resolvers"]
)
abstract class GraphQLTestWithoutDbhAndSkap {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun initialize() {
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(10)).build()
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
    }

    @AfterEach
    fun shutdown() = clearAllMocks()
}

@ActiveProfiles("with-dbh-and-skap")
abstract class GraphQLTestWithDbhAndSkap : GraphQLTestWithoutDbhAndSkap()
