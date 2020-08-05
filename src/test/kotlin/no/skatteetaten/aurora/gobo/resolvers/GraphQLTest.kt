package no.skatteetaten.aurora.gobo.resolvers


import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import no.skatteetaten.aurora.gobo.security.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class GraphQLTestWithoutDbhAndSkap {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun initialize() {
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } answers { User("123456",firstArg(), "Test Testtesen") }
    }

    @AfterEach
    fun shutdown() = clearAllMocks()
}

@ActiveProfiles("with-dbh-and-skap")
abstract class GraphQLTestWithDbhAndSkap : GraphQLTestWithoutDbhAndSkap()
