package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.webflux.AuroraRequestParser.KORRELASJONSID_FIELD
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class MDCQuery : Query {
    fun mdc(dfe: DataFetchingEnvironment): CompletableFuture<String> {
        MDC.put(KORRELASJONSID_FIELD, "123")
        return dfe.loadValue(key = KORRELASJONSID_FIELD, loaderClass = MDCDataLoader::class)
    }
}

@Component
class MDCDataLoader : GoboDataLoader<String, String>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, String> {
        return mapOf(KORRELASJONSID_FIELD to MDC.get(KORRELASJONSID_FIELD))
    }
}

@Import(MDCQuery::class, MDCDataLoader::class)
class MDCTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/queries/mdc.graphql")
    private lateinit var mdcAndTraceQuery: Resource

    @Test
    fun `Korrelasjonsid is set in dataloader context`() {
        webTestClient
            .queryGraphQL(mdcAndTraceQuery)
            .expectBody()
            .graphqlData("mdc").isEqualTo("123")
            .graphqlDoesNotContainErrors()
    }
}
