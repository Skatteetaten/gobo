package no.skatteetaten.aurora.gobo.infrastructure.client

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.ClientService
import no.skatteetaten.aurora.gobo.domain.model.ClientDto
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap

private val logger = KotlinLogging.logger {}

@Profile("local")
@Service
class ClientServiceInMemory : ClientService {
    private val clients = LinkedMultiValueMap<String, ClientDto>()

    override fun addClient(client: ClientDto) {
        logger.trace("Adding client:$client")
        clients.add(client.name, client)
    }

    override fun getClientWithName(name: String) =
        clients[name]?.let { c ->
            ClientDto(
                name = name,
                count = c.sumOf { it.count }
            )
        }

    override fun getAllClients() = clients.keys.map { getClientWithName(it)!! }

    override fun insertOrUpdateClient(client: ClientDto) {
        addClient(client)
    }
}
