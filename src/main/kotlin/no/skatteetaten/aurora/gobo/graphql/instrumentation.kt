package no.skatteetaten.aurora.gobo.graphql

import graphql.ExecutionInput
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.nextgen.InstrumentationExecutionParameters
import graphql.language.Field
import graphql.language.SelectionSet
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
/* HER
private val logger = KotlinLogging.logger {}

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
//
//class UserUsage {
//    val users: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()
//
//    fun update(executionContext: ExecutionContext?) {
//        val context = executionContext?.context
//        if (context is GraphQLServletContext) {
//            val user = context.httpServletRequest.currentUser()
//            users.computeIfAbsent(user.id) { LongAdder() }.increment()
//        }
//    }
}



/*
private val logger = KotlinLogging.logger {}

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
        val context = executionContext?.context
        if (context is GraphQLServletContext) {
            val user = context.httpServletRequest.currentUser()
            users.computeIfAbsent(user.id) { LongAdder() }.increment()
        }
    }
}
*/
