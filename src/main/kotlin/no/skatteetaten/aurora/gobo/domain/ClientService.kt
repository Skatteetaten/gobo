package no.skatteetaten.aurora.gobo.domain

import no.skatteetaten.aurora.gobo.domain.model.ClientDto

interface ClientService {
    fun addClient(client: ClientDto)

    fun getClientWithName(name: String): ClientDto?

    fun getAllClients(): List<ClientDto>

    fun insertOrUpdateClient(client: ClientDto)
}
