package no.skatteetaten.aurora.gobo.graphql

import graphql.ExecutionInput
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.language.Field
import graphql.language.SelectionSet
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger { }

fun String.removeNewLines() =
    this.replace("\n", " ")
        .replace(Regex("\\s+"), " ")

@Component
class GoboInstrumentation(val fieldService: FieldService) : SimpleInstrumentation() {

    val fieldUsage = FieldUsage(fieldService)
    val userUsage = UserUsage()

    @PostConstruct
    fun gurre() {
        print("hei")
    }

    override fun instrumentExecutionInput(
        executionInput: ExecutionInput?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionInput {
        executionInput?.let {
            val request = (executionInput.context as GoboGraphQLContext).request
            logger.debug("Request hostName=\"${request.remoteAddress?.hostName}\"")
            val requestInfo =
                "Klientid=\"${request.klientid()}\" Korrelasjonsid=\"${request.korrelasjonsid()}\""

            val query = it.query.removeNewLines()
            if (!query.startsWith("query IntrospectionQuery")) {
                if (query.trimStart().startsWith("mutation")) {
                    logger.info("$requestInfo mutation=\"$query\" - variable-keys=${it.variables.keys}")
                } else {
                    val variables = if (it.variables.isEmpty()) "" else " - variables=${it.variables}"
                    logger.info("$requestInfo query=\"$query\"$variables")
                }
            }
        }
        return super.instrumentExecutionInput(executionInput, parameters)
    }

    override fun instrumentExecutionContext(
        executionContext: ExecutionContext?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionContext {
        val selectionSet = executionContext?.operationDefinition?.selectionSet ?: SelectionSet(emptyList())
        if (selectionSet.selections.isNotEmpty() && selectionSet.isNotIntrospectionQuery()) {
            fieldUsage.update(selectionSet)
            userUsage.update(executionContext)
        }
        return super.instrumentExecutionContext(executionContext, parameters)
    }

    private fun SelectionSet.isNotIntrospectionQuery(): Boolean {
        val selection = this.selections?.first()
        return !(selection is Field && selection.name.startsWith("__schema"))
    }
}

class FieldUsage(val fieldService: FieldService) {

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

    fun insertOrUpdateFieldUsage() {
        fields.map {
            fieldService.insertOrUpdateField(FieldDto(it.key, it.value.toInt()))
        }
    }
}

class UserUsage {
    val users: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()

    fun update(executionContext: ExecutionContext?) {
        try {
            executionContext?.getContext<GoboGraphQLContext>()?.request?.klientid()?.let {
                users.computeIfAbsent(it) { LongAdder() }.increment()
            }
        } catch (e: Throwable) {
            logger.warn(e) { "Unable to get GraphQL context: " }
        }
    }
}

fun HttpRequest?.klientid() =
    this?.headers?.getFirst(AuroraRequestParser.KLIENTID_FIELD) ?: this?.headers?.getFirst(HttpHeaders.USER_AGENT)

fun HttpRequest?.korrelasjonsid() =
    this?.headers?.getFirst(AuroraRequestParser.KORRELASJONSID_FIELD)
