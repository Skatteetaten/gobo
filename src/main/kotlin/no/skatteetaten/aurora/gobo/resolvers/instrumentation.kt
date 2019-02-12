package no.skatteetaten.aurora.gobo.resolvers

import graphql.ExecutionInput
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.language.Field
import graphql.language.SelectionSet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GoboInstrumentation : SimpleInstrumentation() {

    private val logger = LoggerFactory.getLogger(GoboInstrumentation::class.java)

    val fields = Fields()

    override fun instrumentExecutionInput(
        executionInput: ExecutionInput?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionInput {
        executionInput?.let {
            val query = it.query
            if (query.trimStart().startsWith("mutation")) {
                logger.info("Mutation: $query")
            } else {
                logger.info("Query: $query - Variables: ${it.variables}")
            }
        }
        return super.instrumentExecutionInput(executionInput, parameters)
    }

    override fun instrumentExecutionContext(
        executionContext: ExecutionContext?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionContext {
        val selectionSet = executionContext?.operationDefinition?.selectionSet ?: SelectionSet(emptyList())
        fields.updateFieldNames(selectionSet)

        return super.instrumentExecutionContext(executionContext, parameters)
    }
}

class Fields {
    private val _names: MutableSet<String> = mutableSetOf()
    val names: Set<String>
        get() = _names.toSet()

    fun updateFieldNames(selectionSet: SelectionSet?, parent: String? = null) {
        selectionSet?.selections?.map {
            val name = (it as Field).name
            val fullName = if (parent == null) name else "$parent.$name"
            _names.add(fullName)
            updateFieldNames(it.selectionSet, fullName)
        }
    }
}