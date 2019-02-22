package no.skatteetaten.aurora.gobo.resolvers

import graphql.ExecutionInput
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.language.Field
import graphql.language.SelectionSet
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class GoboInstrumentation : SimpleInstrumentation() {

    val fields = Fields()

    override fun instrumentExecutionInput(
        executionInput: ExecutionInput?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionInput {
        executionInput?.let {
            val query = it.query
            if (query.trimStart().startsWith("mutation")) {
                logger.info("mutation=\"$query\" - variable-keys=${it.variables.keys}")
            } else {
                logger.info("query=\"$query\" - variables=${it.variables}")
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
        get() = _names.sorted().toSet()

    fun updateFieldNames(selectionSet: SelectionSet?, parent: String? = null) {
        selectionSet?.selections?.map {
            if (it is Field) {
                val fullName = if (parent == null) it.name else "$parent.${it.name}"
                _names.add(fullName)
                updateFieldNames(it.selectionSet, fullName)
            }
        }
    }
}