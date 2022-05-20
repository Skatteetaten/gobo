package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.spring.GraphQLAutoConfiguration
import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.MeterRegistry
import no.skatteetaten.aurora.gobo.GoboLiveness
import no.skatteetaten.aurora.gobo.GraphQLConfig
import no.skatteetaten.aurora.gobo.graphql.errorhandling.GoboDataFetcherExceptionHandler
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import no.skatteetaten.aurora.gobo.security.WebSecurityConfig
import no.skatteetaten.aurora.kubernetes.KubernetesReactorClient
import no.skatteetaten.aurora.kubernetes.config.ClientTypes
import no.skatteetaten.aurora.kubernetes.config.TargetClient
import no.skatteetaten.aurora.springboot.AuroraSpringSecurityConfig
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.stereotype.Component
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

const val PROFILE_WITH_DBH_AND_SKAP = "with-dbh-and-skap"

@Component
class TestDummyQuery : Query {
    fun test() = "Dummy query for unit tests"
}

@WithMockUser
@WebFluxTest
@Import(
    GraphQLConfig::class,
    AuroraSpringSecurityConfig::class,
    GoboGraphQLContextFactory::class,
    GoboDataFetcherExceptionHandler::class,
    WebSecurityConfig::class,
    GraphQLAutoConfiguration::class,
    GoboInstrumentation::class,
    TestDummyQuery::class,
    QueryReporter::class,
)
abstract class GraphQLTestWithoutDbhAndSkap {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @TargetClient(ClientTypes.SERVICE_ACCOUNT)
    @MockkBean(relaxed = true)
    protected lateinit var kubernetesReactorClient: KubernetesReactorClient

    @MockkBean(relaxed = true)
    private lateinit var fieldService: FieldService

    @MockkBean(relaxed = true)
    private lateinit var clientService: ClientService

    @MockkBean(relaxed = true)
    private lateinit var meterRegistry: MeterRegistry

    @MockkBean(relaxed = true)
    private lateinit var goboLiveness: GoboLiveness

    @MockkBean(relaxed = true)
    private lateinit var goboMetrics: GoboMetrics

    @BeforeEach
    fun setUpAll() {
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(10)).build()
    }
}

@ActiveProfiles(PROFILE_WITH_DBH_AND_SKAP)
abstract class GraphQLTestWithDbhAndSkap : GraphQLTestWithoutDbhAndSkap()
