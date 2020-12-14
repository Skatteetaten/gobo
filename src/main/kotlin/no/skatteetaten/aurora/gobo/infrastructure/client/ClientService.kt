package no.skatteetaten.aurora.gobo.infrastructure.client

import no.skatteetaten.aurora.gobo.infrastructure.client.repository.ClientRepository
import org.springframework.stereotype.Service

@Service
class ClientService(private val clientRepository: ClientRepository) {

    fun addClient(client: Client) {
        clientRepository.save(client)
    }

    fun getClientWithName(name: String): List<Client> =
        clientRepository.findWithName(name)

    fun getAllClients(): List<Client> =
        clientRepository.findAll()

    fun insertOrUpdateClient(client: Client) {
        clientRepository.incrementCounter(client.name, client.count).takeIfInsertRequired {
            clientRepository.save(client)
        }
    }

    fun getClientCount() = clientRepository.count()

    private fun Int.takeIfInsertRequired(fn: () -> Unit = {}) {
        if (this == 0) {
            fn()
        }
    }
}
