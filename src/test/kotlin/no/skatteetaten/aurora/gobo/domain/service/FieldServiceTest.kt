package no.skatteetaten.aurora.gobo.domain.service

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import no.skatteetaten.aurora.gobo.infrastructure.FieldServiceImpl
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldClientRepository
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [FieldClientRepository::class, FieldServiceImpl::class, FieldRepository::class])
@DataJpaTest
class FieldServiceTest {

    @Autowired
    private lateinit var service: FieldService

    private val field1 = FieldDto(
        name = "gobo.usage.usedFields",
        count = 10,
        clients = listOf(FieldClientDto("donald", 5), FieldClientDto("duck", 5))
    )

    private val field2 = FieldDto(
        name = "gobo.usage.usedFields.name",
        count = 40,
        clients = listOf(FieldClientDto("donald", 40))
    )

    @Test
    fun `Save new field`() {
        service.addField(field1)

        val field = service.getFieldWithName(field1.name)!!
        assertThat(field.name).isEqualTo("gobo.usage.usedFields")
        assertThat(field.count).isEqualTo(10)
        assertThat(field.clients).hasSize(2)
        assertThat(field.clients[0].count).isEqualTo(5)
    }

    @Test
    fun `Update existing field with new count and client`() {
        service.addField(field1)
        val updatedField = field1.copy(count = 12, clients = listOf(FieldClientDto("donald", 12)))
        service.insertOrUpdateField(updatedField)

        val persistedField = service.getFieldWithName(updatedField.name)!!
        assertThat(persistedField.name).isEqualTo("gobo.usage.usedFields")
        assertThat(persistedField.count).isEqualTo(22)
    }

    @Test
    fun `Get all persisted fields and clients`() {
        service.insertOrUpdateField(field1)
        service.insertOrUpdateField(field2)

        val fields = service.getAllFields()
        assertThat(fields.size).isEqualTo(2)
        assertThat(fields[0]).isEqualTo(field1)
        assertThat(fields[1]).isEqualTo(field2)
        assertThat(fields[0].clients).hasSize(2)
        assertThat(fields[1].clients).hasSize(1)
    }

    @Test
    fun `Throw exception when trying to add same field twice`() {
        service.addField(field1)
        assertThat { service.addField(field1) }.isFailure().isInstanceOf(DuplicateKeyException::class)
    }
}
