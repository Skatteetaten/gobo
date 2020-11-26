package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.spring.GraphQLAutoConfiguration
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.DataLoaderConfiguration
import no.skatteetaten.aurora.gobo.GraphQLConfig
import no.skatteetaten.aurora.gobo.graphql.errorhandling.GoboDataFetcherExceptionHandler
import no.skatteetaten.aurora.gobo.security.GoboSecurityContextRepository
import no.skatteetaten.aurora.gobo.security.OpenShiftAuthenticationManager
import no.skatteetaten.aurora.gobo.security.WebSecurityConfig
import no.skatteetaten.aurora.kubernetes.KubernetesReactorClient
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

const val PROFILE_WITH_DBH_AND_SKAP = "with-dbh-and-skap"

@WithMockUser
@WebFluxTest
@Import(
    GraphQLConfig::class,
    DataLoaderConfiguration::class,
    OpenShiftAuthenticationManager::class,
    GoboSecurityContextRepository::class,
    GoboGraphQLContextFactory::class,
    GoboDataFetcherExceptionHandler::class,
    WebSecurityConfig::class,
    GraphQLAutoConfiguration::class
)
abstract class GraphQLTestWithoutDbhAndSkap {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @MockkBean(relaxed = true)
    protected lateinit var kubernetesReactorClient: KubernetesReactorClient

    @BeforeEach
    fun setUpAll() {
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(10)).build()
    }
}

@ActiveProfiles(PROFILE_WITH_DBH_AND_SKAP)
abstract class GraphQLTestWithDbhAndSkap : GraphQLTestWithoutDbhAndSkap()
