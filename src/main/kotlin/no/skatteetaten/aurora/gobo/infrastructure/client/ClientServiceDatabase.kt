package no.skatteetaten.aurora.gobo.infrastructure.client

import no.skatteetaten.aurora.gobo.domain.ClientService
import no.skatteetaten.aurora.gobo.domain.model.ClientDto
import no.skatteetaten.aurora.gobo.infrastructure.client.repository.ClientRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("!local")
@Service
class ClientServiceDatabase(private val clientRepository: ClientRepository) : ClientService {

    override fun addClient(client: ClientDto) {
        clientRepository.save(client)
    }

    override fun getClientWithName(name: String): ClientDto? =
        clientRepository.findByName(name)

    override fun getAllClients(): List<ClientDto> =
        clientRepository.findAll()

    override fun insertOrUpdateClient(client: ClientDto) {
        clientRepository.incrementCounter(client.name, client.count).takeIfInsertRequired {
            clientRepository.save(client)
        }
    }

    private fun Int.takeIfInsertRequired(fn: () -> Unit = {}) {
        if (this == 0) {
            fn()
        }
    }
}
