package no.skatteetaten.aurora.gobo.infrastructure.client

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import no.skatteetaten.aurora.gobo.infrastructure.client.repository.ClientRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [ClientRepository::class, ClientService::class])
@DataJpaTest
class ClientServiceTest {

    @Autowired
    private lateinit var service: ClientService

    private val client1 = Client(
        name = "donald",
        count = 5
    )

    private val client2 = Client(
        name = "joe",
        count = 10
    )

    @Test
    fun `Save new client`() {
        service.addClient(client1)

        val field = service.getClientWithName(client1.name).first()
        assertThat(field.name).isEqualTo("donald")
        assertThat(field.count).isEqualTo(5)
    }

    @Test
    fun `Update existing client with new count`() {
        service.addClient(client1)
        val updatedClient = client1.copy(count = 12)
        service.insertOrUpdateClient(updatedClient)

        val persistedField = service.getClientWithName(updatedClient.name).first()
        assertThat(persistedField.name).isEqualTo("donald")
        assertThat(persistedField.count).isEqualTo(17)
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

    @Test
    fun `Throw exception when trying to add same field twice`() {
        service.addClient(client1)
        assertThat { service.addClient(client1) }.isFailure().isInstanceOf(DuplicateKeyException::class)
    }

    @Test
    fun `Get client with name containing`() {
        service.addClient(client1)
        service.addClient(client2)

        val results1 = service.getClientWithName("o")
        val results2 = service.getClientWithName("don")

        assertThat(results1).hasSize(2)
        assertThat(results2).hasSize(1)
    }
}
