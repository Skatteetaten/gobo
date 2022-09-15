package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.skatteetaten.aurora.webflux.AuroraRequestParser.KORRELASJONSID_FIELD
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class MDCQuery : Query {
    suspend fun mdc(dfe: DataFetchingEnvironment): CompletableFuture<String?> {
        withContext(MDCContext()) {
            MDC.put(KORRELASJONSID_FIELD, "123")
        }
        return dfe.loadValue(key = KORRELASJONSID_FIELD, loaderClass = MDCDataLoader::class)
    }
}

@Component
class MDCDataLoader : GoboDataLoader<String, String?>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, String?> {
        return mapOf(KORRELASJONSID_FIELD to MDC.get(KORRELASJONSID_FIELD))
    }
}

@Import(MDCQuery::class, MDCDataLoader::class)
class MDCTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/queries/mdc.graphql")
    private lateinit var mdcQuery: Resource

    @Test
    @Disabled("MDC value is null, needs to be fixed")
    fun `Korrelasjonsid is set in dataloader context`() {
        webTestClient
            .queryGraphQL(mdcQuery)
            .expectBody()
            .printResult()
                /*
            .graphqlData("mdc").isEqualTo("123")
            .graphqlDoesNotContainErrors()

                 */
    }
}
