package no.skatteetaten.aurora.gobo.resolvers

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import graphql.execution.ExecutionContextBuilder
import graphql.execution.ExecutionId
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.language.SelectionSet
import org.junit.jupiter.api.Test

class GoboInstrumentationTest {

    private val goboInstrumentation = GoboInstrumentation()

    @Test
    fun `Find field names from ExecutionContext`() {
        val selectionSet = SelectionSet.newSelectionSet().selections(listOf(Field("id"))).build()
        val operationDefinition = OperationDefinition.newOperationDefinition().selectionSet(selectionSet).build()
        val executionContext =
            ExecutionContextBuilder.newExecutionContextBuilder()
                .executionId(ExecutionId.from("123"))
                .operationDefinition(operationDefinition).build()
        goboInstrumentation.instrumentExecutionContext(executionContext, null)
        assertThat(goboInstrumentation.fields.names).hasSize(1)
        assertThat(goboInstrumentation.fields.names).contains("id")
    }

    @Test
    fun `No field names found when SelectionSet is empty in ExecutionContext`() {
        val executionContext =
            ExecutionContextBuilder()
                .executionId(ExecutionId.from("123"))
                .operationDefinition(OperationDefinition.newOperationDefinition().build()).build()
        goboInstrumentation.instrumentExecutionContext(executionContext, null)
        assertThat(goboInstrumentation.fields.names).hasSize(0)
    }
}