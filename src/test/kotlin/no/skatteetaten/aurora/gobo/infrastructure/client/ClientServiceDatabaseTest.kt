package no.skatteetaten.aurora.gobo.infrastructure.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import no.skatteetaten.aurora.gobo.domain.ClientService
import no.skatteetaten.aurora.gobo.domain.model.ClientDto
import no.skatteetaten.aurora.gobo.infrastructure.client.repository.ClientRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [ClientRepository::class, ClientServiceDatabase::class])
@DataJpaTest
class ClientServiceDatabaseTest {

    @Autowired
    private lateinit var service: ClientService

    private val client1 = ClientDto(
        name = "donald",
        count = 5
    )

    private val client2 = ClientDto(
        name = "joe",
        count = 10
    )

    @Test
    fun `Save new client`() {
        service.addClient(client1)

        val field = service.getClientWithName(client1.name)!!
        assertThat(field.name).isEqualTo("donald")
        assertThat(field.count).isEqualTo(5)
    }

    @Test
    fun `Update existing client with new count`() {
        service.addClient(client1)
        val updatedClient = client1.copy(count = 12)
        service.insertOrUpdateClient(updatedClient)

        val persistedField = service.getClientWithName(updatedClient.name)!!
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
}
