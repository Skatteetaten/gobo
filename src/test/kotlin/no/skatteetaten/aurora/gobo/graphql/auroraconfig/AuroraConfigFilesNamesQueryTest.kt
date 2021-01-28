package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService

@Import(
    AffiliationQuery::class,
    AuroraConfigDataLoader::class
)
class AuroraConfigFilesNamesQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getAffiliationAuroraConfigFilesWithNames.graphql")
    private lateinit var getAffiliationAuroraConfigFilesWithNamesQuery: Resource

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @Test
    fun `Query for all file names in the auroraconfig of an affiliation`() {
        val file = AuroraConfigFileResource("about.foo", "{}", AuroraConfigFileType.DEFAULT, "789")
        val auroraConfig = AuroraConfig("aurora", "master", "qwerty", listOf(file))

        coEvery { auroraConfigService.getAuroraConfig(any(), eq("aurora"), any()) } returns auroraConfig

        webTestClient.queryGraphQL(getAffiliationAuroraConfigFilesWithNamesQuery, mapOf("name" to "aurora"), token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.items") {
                graphqlData("[0].name").isEqualTo("aurora")
                graphqlData("[0].auroraConfig.name").isEqualTo("aurora")
                graphqlData("[0].auroraConfig.files[0].name").isEqualTo("about.foo")
            }
            .graphqlDoesNotContainErrors()
    }
}
