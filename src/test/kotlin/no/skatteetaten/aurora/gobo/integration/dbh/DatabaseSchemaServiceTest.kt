package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.catch
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.SchemaCreationRequestBuilder
import no.skatteetaten.aurora.gobo.resolvers.MissingLabelException
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class DatabaseSchemaServiceTest {
    private val sharedSecretReader = mockk<SharedSecretReader>().apply {
        every { secret } returns "secret"
    }
    private val databaseSchemaService = DatabaseSchemaService(sharedSecretReader, WebClient.create())

    @Test
    fun `Create database schema given missing labels throw exception`() {
        val exception =
            catch { databaseSchemaService.createDatabaseSchema(SchemaCreationRequestBuilder(labels = emptyMap()).build()) }
        assertThat(exception).isNotNull().isInstanceOf(MissingLabelException::class)
    }
}