package no.skatteetaten.aurora.gobo.resolvers

// FIXME instrumentation test
/*
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
*/
