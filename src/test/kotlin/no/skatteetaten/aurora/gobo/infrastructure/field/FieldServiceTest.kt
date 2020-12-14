package no.skatteetaten.aurora.gobo.infrastructure.field

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import no.skatteetaten.aurora.gobo.infrastructure.field.repository.FieldClientRepository
import no.skatteetaten.aurora.gobo.infrastructure.field.repository.FieldRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [FieldClientRepository::class, FieldService::class, FieldRepository::class])
@DataJpaTest
class FieldServiceTest {

    @Autowired
    private lateinit var service: FieldService

    private val field1 = Field(
        name = "gobo.usage.usedFields",
        count = 10,
        clients = listOf(FieldClient("donald", 5), FieldClient("duck", 5))
    )

    private val field2 = Field(
        name = "gobo.usage.usedFields.name",
        count = 40,
        clients = listOf(FieldClient("donald", 40))
    )

    @Test
    fun `Save new field`() {
        service.addField(field1)

        val field = service.getFieldWithName(field1.name).first()
        assertThat(field.name).isEqualTo("gobo.usage.usedFields")
        assertThat(field.count).isEqualTo(10)
        assertThat(field.clients).hasSize(2)
        assertThat(field.clients[0].count).isEqualTo(5)
    }

    @Test
    fun `Update existing field with new count and client`() {
        service.addField(field1)
        val updatedField = field1.copy(count = 12, clients = listOf(FieldClient("donald", 12)))
        service.insertOrUpdateField(updatedField)

        val persistedField = service.getFieldWithName(updatedField.name).first()
        assertThat(persistedField.name).isEqualTo("gobo.usage.usedFields")
        assertThat(persistedField.count).isEqualTo(22)
        assertThat(persistedField.clients[0].count).isEqualTo(17)
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

    @Test
    fun `Get field with name containing`() {
        service.addField(field1)
        service.addField(field2)

        val result1 = service.getFieldWithName("gobo")
        val result2 = service.getFieldWithName("gobo.usage.usedFields.name")

        assertThat(result1).hasSize(2)
        assertThat(result2).hasSize(1)
    }

    @Test
    fun `Get field count`() {
        service.addField(field1)
        service.addField(field2)

        val fieldCount = service.getFieldCount()
        assertThat(fieldCount).isEqualTo(2)
    }
}
