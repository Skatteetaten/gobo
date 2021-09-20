package no.skatteetaten.aurora.gobo.graphql

import graphql.ExecutionInput
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.language.SelectionSet
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.gobo.GoboFieldUser
import no.skatteetaten.aurora.gobo.infrastructure.client.Client
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import no.skatteetaten.aurora.gobo.infrastructure.field.Field
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldClient
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import no.skatteetaten.aurora.gobo.removeNewLines
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger { }

private fun String.isNotIntrospectionQuery() = !startsWith("query IntrospectionQuery")

@Component
class GoboInstrumentation(
    private val fieldService: FieldService,
    private val clientService: ClientService
) : SimpleInstrumentation() {
    val fieldUsage = FieldUsage()
    val clientUsage = ClientUsage()

    @PreDestroy
    fun update() {
        val numOfFields = fieldUsage.fields.entries.count { it.value.sum() > 0 }
        val numOfClients = clientUsage.clients.entries.count { it.value.sum() > 0 }
        if (numOfFields > 0 || numOfClients > 0) {
            logger.info("Updating $numOfFields fields and $numOfClients clients")
        }

        fieldUsage.getAndResetFieldUsage().forEach {
            fieldService.insertOrUpdateField(it)
        }

        clientUsage.getAndResetClientUsage().forEach {
            clientService.insertOrUpdateClient(it)
        }
    }

    override fun instrumentExecutionInput(
        executionInput: ExecutionInput?,
        parameters: InstrumentationExecutionParameters?
    ): ExecutionInput {
        executionInput?.let {
            val context = (executionInput.context as GoboGraphQLContext)
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
                logger.debug { queryText }
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
            fieldUsage.update(executionContext, selectionSet)
            clientUsage.update(executionContext)
        }
        return super.instrumentExecutionContext(executionContext, parameters)
    }

    private fun SelectionSet.isNotIntrospectionQuery(): Boolean {
        val selection = this.selections?.first()
        return !(selection is graphql.language.Field && selection.name.startsWith("__schema"))
    }
}

class FieldUsage {
    private val _fieldUsers: ConcurrentHashMap<GoboFieldUser, LongAdder> = ConcurrentHashMap()
    private val _fields: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()
    val fields: Map<String, LongAdder>
        get() = _fields.toSortedMap()

    fun update(executionContext: ExecutionContext?, selectionSet: SelectionSet?, parent: String? = null) {
        selectionSet?.selections?.map {
            if (it is graphql.language.Field) {
                val fullName = if (parent == null) it.name else "$parent.${it.name}"
                val clientId: String? = executionContext?.getContext<GoboGraphQLContext>()?.request?.klientid()
                _fields.computeIfAbsent(fullName) { LongAdder() }.increment()
                clientId?.let { it1 -> GoboFieldUser(fullName, it1) }
                    ?.let { it2 -> _fieldUsers.computeIfAbsent(it2) { LongAdder() }.increment() }
                update(executionContext, it.selectionSet, fullName)
            }
        }
    }

    fun getAndResetFieldUsage() =
        fields.map { field ->
            val keys = _fieldUsers.keys.filter { field.key == it.name }
            val clients = keys.map { FieldClient(it.user, _fieldUsers[it]?.sumThenReset() ?: 0) }
            Field(
                name = field.key,
                count = field.value.sumThenReset(),
                clients = clients
            )
        }
}

class ClientUsage {
    val clients: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()

    fun update(executionContext: ExecutionContext?) {
        try {
            executionContext?.getContext<GoboGraphQLContext>()?.request?.klientid()?.let {
                clients.computeIfAbsent(it) { LongAdder() }.increment()
            }
        } catch (e: Throwable) {
            logger.warn(e) { "Unable to get GraphQL context: " }
        }
    }

    fun getAndResetClientUsage() =
        clients.map {
            Client(
                name = it.key,
                count = it.value.sumThenReset()
            )
        }
}

fun ServerRequest?.klientid() =
    this?.headers()?.firstHeader(AuroraRequestParser.KLIENTID_FIELD) ?: this?.headers()
        ?.firstHeader(HttpHeaders.USER_AGENT)

fun ServerRequest?.korrelasjonsid() =
    this?.headers()?.firstHeader(AuroraRequestParser.KORRELASJONSID_FIELD)
