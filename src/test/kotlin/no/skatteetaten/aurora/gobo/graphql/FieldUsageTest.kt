package no.skatteetaten.aurora.gobo.graphql

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import graphql.language.Field
import graphql.language.SelectionSet
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.domain.FieldService
import org.junit.jupiter.api.Test

class FieldUsageTest {
    private val fieldService: FieldService = mockk()
    private val usage = FieldUsage(fieldService)

    @Test
    fun `Get field name from SelectionSet`() {

        val id = SelectionSet.newSelectionSet().selections(listOf(Field("id"), Field("id"))).build()
        val databaseSchema = SelectionSet.newSelectionSet().selections(listOf(Field("databaseSchema", id))).build()
        usage.update(databaseSchema)

        val fields = usage.fields
        assertThat(fields.keys).all {
            hasSize(2)
            contains("databaseSchema")
            contains("databaseSchema.id")
        }
        assertThat(fields["databaseSchema"]?.sum()).isEqualTo(1L)
        assertThat(fields["databaseSchema.id"]?.sum()).isEqualTo(2L)
    }
}
