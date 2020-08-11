package no.skatteetaten.aurora.gobo.integration.dbh

import no.skatteetaten.aurora.gobo.SchemaCreationRequestBuilder
import no.skatteetaten.aurora.gobo.resolvers.MissingLabelException
import no.skatteetaten.aurora.gobo.testObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

class DatabaseServiceTest {
    private val databaseSchemaService = DatabaseServiceReactive(WebClient.create(), testObjectMapper())

    @Test
    fun `Create database schema given missing labels throw exception`() {
        val response =
            databaseSchemaService.createDatabaseSchema(SchemaCreationRequestBuilder(labels = emptyMap()).build())

        StepVerifier.create(response)
            .expectErrorMatches { it is MissingLabelException }
            .verify()
    }
}
