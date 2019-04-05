package no.skatteetaten.aurora.gobo.resolvers

import graphql.ExecutionInput
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.language.Field
import graphql.language.SelectionSet
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

private val logger = KotlinLogging.logger {}

fun String.removeNewLines() =
    this.replace("\n", " ")
        .replace(Regex("\\s+"), " ")

@Component
class GoboInstrumentation : SimpleInstrumentation() {

    val usage = Usage()

    override fun instrumentExecutionInput(
        executionInput: ExecutionInput?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionInput {
        executionInput?.let {
            val query = it.query.removeNewLines()
            if (query.trimStart().startsWith("mutation")) {
                logger.info("mutation=\"$query\" - variable-keys=${it.variables.keys}")
            } else {
                val variables = if (it.variables.isEmpty()) "" else " - variables=${it.variables}"
                logger.info("query=\"$query\"$variables")
            }
        }
        return super.instrumentExecutionInput(executionInput, parameters)
    }

    override fun instrumentExecutionContext(
        executionContext: ExecutionContext?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionContext {
        val selectionSet = executionContext?.operationDefinition?.selectionSet ?: SelectionSet(emptyList())
        usage.updateFieldNames(selectionSet)
        return super.instrumentExecutionContext(executionContext, parameters)
    }
}

class Usage {
    private val _fields: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()
    val fields: Map<String, LongAdder>
        get() = _fields.toSortedMap()

    fun updateFieldNames(selectionSet: SelectionSet?, parent: String? = null) {
        selectionSet?.selections?.map {
            if (it is Field) {
                val fullName = if (parent == null) it.name else "$parent.${it.name}"
                _fields.computeIfAbsent(fullName) { LongAdder() }.increment()
                updateFieldNames(it.selectionSet, fullName)
            }
        }
    }
}