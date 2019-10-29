package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.assertThat
import assertk.assertions.containsAll
import no.skatteetaten.aurora.gobo.SchemaCreationRequestBuilder
import org.junit.jupiter.api.Test

class SchemaCreationRequestTest {

    @Test
    fun `Find missing labels`() {
        val schemaCreationRequest =
            SchemaCreationRequestBuilder(labels = mapOf("affiliation" to "paas", "environment" to "test")).build()
        val missingLabels = schemaCreationRequest.findMissingOrEmptyLabels()
        assertThat(missingLabels).containsAll("application", "name")
    }

    @Test
    fun `Find empty labels`() {
        val schemaCreationRequest =
            SchemaCreationRequestBuilder(
                labels = mapOf(
                    "affiliation" to "paas",
                    "environment" to "test",
                    "application" to "",
                    "name" to ""
                )
            ).build()
        val missingLabels = schemaCreationRequest.findMissingOrEmptyLabels()
        assertThat(missingLabels).containsAll("application", "name")
    }
}
