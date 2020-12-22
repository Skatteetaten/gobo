package no.skatteetaten.aurora.gobo.graphql.user

import com.ninjasquad.springmockk.MockkBean
import io.fabric8.openshift.api.model.User
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(UserFullNameDataLoader::class, CurrentUserQuery::class)
class CurrentUserQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getCurrentUser.graphql")
    private lateinit var getCurrentUserQuery: Resource

    @MockkBean(relaxed = true)
    private lateinit var kubernetesClient: KubernetesCoroutinesClient

    @BeforeEach
    fun setUp() {
        coEvery { kubernetesClient.currentUser(any()) } returns User()
    }

    @Test
    fun `Query for current user`() {
        webTestClient
            .queryGraphQL(getCurrentUserQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("currentUser.id").isNotEmpty
            .graphqlData("currentUser.name").isNotEmpty
            .graphqlDoesNotContainErrors()
    }
}
