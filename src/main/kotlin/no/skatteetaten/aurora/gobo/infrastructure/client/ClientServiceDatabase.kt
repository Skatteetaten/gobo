package no.skatteetaten.aurora.gobo.infrastructure.client

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.ClientService
import no.skatteetaten.aurora.gobo.domain.model.ClientDto
import no.skatteetaten.aurora.gobo.infrastructure.client.repository.ClientRepository
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger {}

@Service
class ClientServiceDatabase(private val clientRepository: ClientRepository) : ClientService {

    @PostConstruct
    fun init() {
        logger.info("Starting client service database integration")
    }

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
