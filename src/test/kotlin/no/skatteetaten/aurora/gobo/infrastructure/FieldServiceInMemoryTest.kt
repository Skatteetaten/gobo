package no.skatteetaten.aurora.gobo.infrastructure

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import org.junit.jupiter.api.Test

class FieldServiceInMemoryTest {
    private val service = FieldServiceInMemory()

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
        assertThat(persistedField.count).isEqualTo(12)
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
}
