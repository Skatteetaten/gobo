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
import no.skatteetaten.aurora.gobo.graphql.gobo.GoboFieldUsage
import no.skatteetaten.aurora.gobo.graphql.gobo.GoboUser
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger { }

fun String.removeNewLines() =
    this.replace("\n", " ")
        .replace(Regex("\\s+"), " ")

private fun String.isNotIntrospectionQuery() = !startsWith("query IntrospectionQuery")

@Component
class GoboInstrumentation(val fieldService: FieldService) : SimpleInstrumentation() {

    val fieldUsage = FieldUsage(fieldService)
    val userUsage = UserUsage()

    @PostConstruct
    fun initateAfterSpringStartup() {
        fieldUsage.initiateFieldUsage()
    }

    @PreDestroy
    fun beforeSpringShutdown() {
        fieldUsage.insertOrUpdateFieldUsage()
    }

    override fun instrumentExecutionInput(
        executionInput: ExecutionInput?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionInput {
        executionInput?.let {
            val context = (executionInput.context as GoboGraphQLContext)
            val request = context.request
            logger.debug { """Request hostName="${request.remoteAddress?.hostName}" """ }

            val queryText = it.query.removeNewLines().let { query ->
                if (query.trimStart().startsWith("mutation")) {
                    """mutation="$query" - variable-keys=${it.variables.keys}"""
                } else {
                    val variables = if (it.variables.isEmpty()) "" else " - variables=${it.variables}"
                    """query="$query"$variables"""
                }
            }

            context.query = queryText
            if (queryText.isNotIntrospectionQuery()) {
                logger.debug(queryText)
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
    private val _fields: ConcurrentHashMap<String, GoboFieldUsage> = ConcurrentHashMap()
    val fields: Map<String, GoboFieldUsage>
        get() = _fields.toSortedMap()
    val clientsMap: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()

    fun update(selectionSet: SelectionSet?, parent: String? = null) {
        selectionSet?.selections?.map {
            if (it is Field) {
                val fullName = if (parent == null) it.name else "$parent.${it.name}"
                clientsMap.computeIfAbsent("donald") { LongAdder() }.increment()

                _fields.computeIfAbsent(fullName) {
                    GoboFieldUsage(
                        name = fullName,
                        clients = clientsMap.map { GoboUser(it.key, it.value.sum()) }
                    )
                }.count.increment()
                update(it.selectionSet, fullName)
            }
        }
    }

    fun initiateFieldUsage() {
//        fieldService.getAllFields().map { _fields.put(it.name, LongAdder().apply { add(it.count) }) }
//        fieldService.getAllFields().map { _fields.put(it.name, GoboFieldCounter().apply { it.count}) }
    }

    fun insertOrUpdateFieldUsage() {
        fields.map {
            print(it)
            fieldService.insertOrUpdateField(FieldDto(name = it.key, count = it.value.count.sum()))
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
