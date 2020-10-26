package no.skatteetaten.aurora.gobo.graphql

import graphql.ExecutionInput
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.language.Field
import graphql.language.SelectionSet
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

private val logger = KotlinLogging.logger { }

fun String.removeNewLines() =
    this.replace("\n", " ")
        .replace(Regex("\\s+"), " ")

@Component
class GoboInstrumentation : SimpleInstrumentation() {

    val fieldUsage = FieldUsage()
    val userUsage = UserUsage()

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
        fieldUsage.update(selectionSet)
        userUsage.update(executionContext)
        return super.instrumentExecutionContext(executionContext, parameters)
    }
}

class FieldUsage {
    private val _fields: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()
    val fields: Map<String, LongAdder>
        get() = _fields.toSortedMap()

    fun update(selectionSet: SelectionSet?, parent: String? = null) {
        selectionSet?.selections?.map {
            if (it is Field) {
                val fullName = if (parent == null) it.name else "$parent.${it.name}"
                _fields.computeIfAbsent(fullName) { LongAdder() }.increment()
                update(it.selectionSet, fullName)
            }
        }
    }
}

class UserUsage {
    val users: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()

    fun update(executionContext: ExecutionContext?) {
        try {
            executionContext?.getContext<GoboGraphQLContext>()?.request?.headers?.let { headers ->
                headers.getFirst("CLIENT_ID")?.let { clientId ->
                    users.computeIfAbsent(clientId) { LongAdder() }.increment()
                } ?: headers.getFirst(HttpHeaders.USER_AGENT)?.let { userAgent ->
                    users.computeIfAbsent(userAgent) { LongAdder() }.increment()
                }
            }
        } catch (e: Throwable) {
            logger.warn(e) { "Unable to get GraphQL context: " }
        }
    }
}
