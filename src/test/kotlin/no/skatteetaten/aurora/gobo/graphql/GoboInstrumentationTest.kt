package no.skatteetaten.aurora.gobo.graphql

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.ninjasquad.springmockk.MockkBean
import graphql.execution.ExecutionContextBuilder
import graphql.execution.ExecutionId
import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.language.SelectionSet
import no.skatteetaten.aurora.gobo.domain.FieldService
import org.junit.Test
import javax.annotation.PostConstruct

class GoboInstrumentationTest {

    @MockkBean
    private lateinit var fieldService: FieldService

//    private val usage = FieldUsage(fieldService)

    private val goboInstrumentation = GoboInstrumentation(fieldService)

    @PostConstruct
    fun gurre() {
        print("hei")
    }

    @Test
    fun `Find field names from ExecutionContext`() {
        val selectionSet = SelectionSet.newSelectionSet().selections(listOf(Field("id"))).build()
        val operationDefinition = OperationDefinition.newOperationDefinition().selectionSet(selectionSet).build()
        val executionContext =
            ExecutionContextBuilder.newExecutionContextBuilder()
                .executionId(ExecutionId.from("123"))
                .operationDefinition(operationDefinition).build()
        goboInstrumentation.instrumentExecutionContext(executionContext, null)
        assertThat(goboInstrumentation.fieldUsage.fields.keys).all {
            hasSize(1)
            contains("id")
        }
        assertThat(goboInstrumentation.fieldUsage.fields["id"]?.sum()).isEqualTo(1L)
    }

    @Test
    fun `No field names found when SelectionSet is empty in ExecutionContext`() {
        val executionContext =
            ExecutionContextBuilder()
                .executionId(ExecutionId.from("123"))
                .operationDefinition(OperationDefinition.newOperationDefinition().build()).build()
        goboInstrumentation.instrumentExecutionContext(executionContext, null)
        assertThat(goboInstrumentation.fieldUsage.fields.entries).hasSize(0)
    }

    @Test
    fun `Remove new lines from query`() {
        val query =
            """
        {
            gobo {
                usage {
                    usedFields {
                        name
                        count
                    }
                }
            }
        }
            """.trimIndent()

        assertThat(query.removeNewLines()).isEqualTo("{ gobo { usage { usedFields { name count } } } }")
    }

    @Test
    fun `Return same string if no new line is in query`() {
        assertThat("{}".removeNewLines()).isEqualTo("{}")
    }
}
