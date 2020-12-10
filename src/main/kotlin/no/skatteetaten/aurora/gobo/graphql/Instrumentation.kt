package no.skatteetaten.aurora.gobo.graphql

import graphql.ExecutionInput
import graphql.execution.ExecutionContext
import graphql.execution.instrumentation.SimpleInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.language.Field
import graphql.language.SelectionSet
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.ClientService
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.ClientDto
import no.skatteetaten.aurora.gobo.graphql.gobo.GoboFieldUser
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import javax.annotation.PreDestroy
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldDto

private val logger = KotlinLogging.logger { }

fun String.removeNewLines() =
    this.replace("\n", " ")
        .replace(Regex("\\s+"), " ")

private fun String.isNotIntrospectionQuery() = !startsWith("query IntrospectionQuery")

@Component
class GoboInstrumentation(
    private val fieldService: FieldService,
    private val clientService: ClientService
) : SimpleInstrumentation() {

    val fieldUsage = FieldUsage(fieldService.getAllFields())
    val clientUsage = ClientUsage(clientService.getAllClients())

    @PreDestroy
    fun update() {
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
            fieldUsage.update(executionContext, selectionSet)
            clientUsage.update(executionContext)
        }
        return super.instrumentExecutionContext(executionContext, parameters)
    }

    private fun SelectionSet.isNotIntrospectionQuery(): Boolean {
        val selection = this.selections?.first()
        return !(selection is Field && selection.name.startsWith("__schema"))
    }
}

class FieldUsage(initialFields: List<FieldDto>) {
    private val _fieldUsers: ConcurrentHashMap<GoboFieldUser, LongAdder> = ConcurrentHashMap()
    private val _fields: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()
    val fields: Map<String, LongAdder>
        get() = _fields.toSortedMap()

    init {
        initialFields.forEach { field ->
            _fields[field.name] = LongAdder().apply { add(field.count) }
            field.clients.forEach { client ->
                _fieldUsers[GoboFieldUser(field.name, client.name)] = LongAdder().apply { add(client.count) }
            }
        }
    }

    fun update(executionContext: ExecutionContext?, selectionSet: SelectionSet?, parent: String? = null) {
        selectionSet?.selections?.map {
            if (it is Field) {
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
            val clients = keys.map { FieldClientDto(it.user, _fieldUsers[it]?.sumThenReset() ?: 0) }
            FieldDto(
                name = field.key,
                count = field.value.sumThenReset(),
                clients = clients
            )
        }
}

class ClientUsage(initialClients: List<ClientDto>) {
    val clients: ConcurrentHashMap<String, LongAdder> = ConcurrentHashMap()

    init {
        initialClients.forEach {
            clients[it.name] = LongAdder().apply { add(it.count) }
        }
    }

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
            ClientDto(
                name = it.key,
                count = it.value.sumThenReset()
            )
        }
}

fun HttpRequest?.klientid() =
    this?.headers?.getFirst(AuroraRequestParser.KLIENTID_FIELD) ?: this?.headers?.getFirst(HttpHeaders.USER_AGENT)

fun HttpRequest?.korrelasjonsid() =
    this?.headers?.getFirst(AuroraRequestParser.KORRELASJONSID_FIELD)
