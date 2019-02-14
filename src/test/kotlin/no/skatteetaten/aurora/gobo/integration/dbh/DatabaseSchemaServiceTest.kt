package no.skatteetaten.aurora.gobo.integration.dbh

import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.SchemaCreationRequestBuilder
import no.skatteetaten.aurora.gobo.resolvers.MissingLabelException
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

class DatabaseSchemaServiceTest {
    private val sharedSecretReader = mockk<SharedSecretReader>().apply {
        every { secret } returns "secret"
    }
    private val databaseSchemaService = DatabaseSchemaService(sharedSecretReader, WebClient.create())

    @Test
    fun `Create database schema given missing labels throw exception`() {
        val response =
            databaseSchemaService.createDatabaseSchema(SchemaCreationRequestBuilder(labels = emptyMap()).build())

        StepVerifier.create(response)
            .expectErrorMatches { it is MissingLabelException }
            .verify()
    }
}