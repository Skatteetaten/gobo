package no.skatteetaten.aurora.gobo.domain.service

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import no.skatteetaten.aurora.gobo.infrastructure.FieldServiceImpl
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldClientRepository
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [FieldClientRepository::class, FieldServiceImpl::class, FieldRepository::class])
@DataJpaTest
class FieldServiceTest {

    @Autowired
    private lateinit var service: FieldService

    @Test
    fun `'addField' should return created entity`() {
        service.addField(
            FieldDto(
                name = "gobo.usage.usedFields.name",
                count = 10,
                clients = listOf(FieldClientDto("donald", 5), FieldClientDto("duck", 5))
            )
        )

        val field = service.getFieldWithName("gobo.usage.usedFields.name")!!
        assertThat(field.name).isEqualTo("gobo.usage.usedFields.name")
        assertThat(field.count).isEqualTo(10)
        assertThat(field.clients).hasSize(2)
        assertThat(field.clients[0].count).isEqualTo(5)
    }

    @Test
    fun `'insertOrUpdateField' first addField and when update field should return created entity`() {
        service.addField(
            FieldDto(
                name = "gobo.usage.usedFields.name",
                count = 10,
                clients = listOf(FieldClientDto("donald", 5), FieldClientDto("duck", 5))
            )
        )
        val insertField = service.getFieldWithName("gobo.usage.usedFields.name")
        assertThat(insertField?.name).isEqualTo("gobo.usage.usedFields.name")
        assertThat(insertField?.count).isEqualTo(10)

        val updatedField = insertField!!.copy(count = 12, clients = listOf(FieldClientDto("donald", 12)))
        service.insertOrUpdateField(updatedField)
        val persistedField = service.getFieldWithName(updatedField.name)
        assertThat(persistedField?.name).isNotNull().isEqualTo("gobo.usage.usedFields.name")
        assertThat(persistedField?.count).isNotNull().isEqualTo(22)
    }

    @Test
    fun `'getAllFields' should return list of created entity`() {
        val tmpField1 = FieldDto(
            name = "gobo.usage.usedFields",
            count = 42,
            clients =
                listOf(FieldClientDto("donald", 42))
        )
        service.insertOrUpdateField(tmpField1)

        val tmpField2 = FieldDto(
            name = "gobo.usage.usedFields.name",
            count = 40,
            clients =
                listOf(FieldClientDto("donald", 40))
        )
        service.insertOrUpdateField(tmpField2)

        val listOfFields = service.getAllFields()
        assertThat(listOfFields.size).isEqualTo(2)
        assertThat(listOfFields[0]).isEqualTo(tmpField1)
        assertThat(listOfFields[1]).isEqualTo(tmpField2)
        assertThat(listOfFields[0].clients).hasSize(1)
        assertThat(listOfFields[1].clients).hasSize(1)
    }
}
