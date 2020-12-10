package no.skatteetaten.aurora.gobo.graphql

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import graphql.execution.ExecutionContextBuilder
import graphql.execution.ExecutionId
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.language.SelectionSet
import org.junit.jupiter.api.Test

class FieldUsageTest {
    private val usage = FieldUsage(emptyList())

    @Test
    fun `Get field name from SelectionSet`() {
        val id = SelectionSet.newSelectionSet().selections(listOf(Field("id"), Field("id"))).build()
        val databaseSchema = SelectionSet.newSelectionSet().selections(listOf(Field("databaseSchema", id))).build()
        val executionContext =
            ExecutionContextBuilder()
                .executionId(ExecutionId.from("123"))
                .operationDefinition(OperationDefinition.newOperationDefinition().build()).build()

        usage.update(executionContext, databaseSchema)

        val fields = usage.fields
        assertThat(fields.keys).all {
            hasSize(2)
            contains("databaseSchema")
            contains("databaseSchema.id")
        }

        fields["databaseSchema"]?.let { assertThat(it.sum()).isEqualTo(1L) }
        fields["databaseSchema.id"]?.let { assertThat(it.sum()).isEqualTo(2L) }
    }
}
