package no.skatteetaten.aurora.gobo.resolvers

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import graphql.language.Field
import graphql.language.SelectionSet
import org.junit.jupiter.api.Test

class FieldsTest {
    private val fields = Fields()

    @Test
    fun `Get field name from SelectionSet`() {
        val id = SelectionSet.newSelectionSet().selections(listOf(Field("id"), Field("id"))).build()
        val databaseSchema = SelectionSet.newSelectionSet().selections(listOf(Field("databaseSchema", id))).build()
        fields.updateFieldNames(databaseSchema)

        assertThat(fields.names).hasSize(2)
        assertThat(fields.names).contains("databaseSchema")
        assertThat(fields.names).contains("databaseSchema.id")
    }
}