package no.skatteetaten.aurora.gobo.infrastructure.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.domain.model.ClientDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ClientServiceInMemoryTest {
    private val service = ClientServiceInMemory()

    private val client1 = ClientDto(
        name = "donald",
        count = 10
    )

    private val client2 = ClientDto(
        name = "joe",
        count = 40
    )

    @Test
    fun `Save new client`() {
        service.addClient(client1)

        val field = service.getClientWithName(client1.name)!!
        assertThat(field.name).isEqualTo("donald")
        assertThat(field.count).isEqualTo(10)
    }

    @Test
    fun `Update existing client with new count`() {
        service.addClient(client1)
        val updatedClient = client1.copy(count = 12)
        service.insertOrUpdateClient(updatedClient)

        val persistedClient = service.getClientWithName(updatedClient.name)!!
        assertThat(persistedClient.name).isEqualTo("donald")
        assertThat(persistedClient.count).isEqualTo(22)
    }

    @Test
    fun `Get all persisted clients`() {
        service.insertOrUpdateClient(client1)
        service.insertOrUpdateClient(client2)

        val clients = service.getAllClients()
        assertThat(clients.size).isEqualTo(2)
        assertThat(clients[0]).isEqualTo(client1)
        assertThat(clients[1]).isEqualTo(client2)
    }
}
