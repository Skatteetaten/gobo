package no.skatteetaten.aurora.gobo.domain.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import no.skatteetaten.aurora.gobo.infrastructure.FieldServiceImpl
import no.skatteetaten.aurora.gobo.infrastructure.InternalFieldConfiguration
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [InternalFieldConfiguration::class, FieldServiceImpl::class, FieldRepository::class])
@DataJpaTest
class FieldServiceTest {

    @Autowired
    private lateinit var service: FieldService

    @Test
    fun `'addField' should return created entity`() {
        val (name, count) = service.addField(FieldDto(name = "gobo.usage.usedFields.name", count = 10))
        assertThat(name).isEqualTo("gobo.usage.usedFields.name")
        assertThat(count).isEqualTo(10)
    }

    @Test
    fun `'insertOrUpdateField' first addField and when update field should return created entity`() {
        val insertField = service.addField(FieldDto(name = "gobo.usage.usedFields.name", count = 10))
        assertThat(insertField.name).isEqualTo("gobo.usage.usedFields.name")
        assertThat(insertField.count).isEqualTo(10)

        val updatedField = insertField.copy(count = 12)
        val (name, count) = service.insertOrUpdateField(updatedField)
        assertThat(name).isEqualTo("gobo.usage.usedFields.name")
        assertThat(count).isEqualTo(12)
    }

    @Test
    fun `'insertOrUpdateField' two times insertOrUpdateField should return created entity`() {
        val tmpField = FieldDto(name = "gobo.usage.usedFields.name", count = 10)
        val insertField = service.insertOrUpdateField(tmpField)
        assertThat(insertField.name).isEqualTo("gobo.usage.usedFields.name")
        assertThat(insertField.count).isEqualTo(10)

        val updatedField = insertField.copy(count = 12)
        val (name, count) = service.insertOrUpdateField(updatedField)
        assertThat(name).isEqualTo("gobo.usage.usedFields.name")
        assertThat(count).isEqualTo(12)
    }

    @Test
    fun `'getAllFields' should return list of created entity`() {
        val tmpField1 = FieldDto(name = "gobo.usage.usedFields", count = 42)
        val insertField1 = service.insertOrUpdateField(tmpField1)

        val tmpField2 = FieldDto(name = "gobo.usage.usedFields.name", count = 40)
        val insertField2 = service.insertOrUpdateField(tmpField2)

        val listOfFields = service.getAllFields()
        assertThat(listOfFields.size).isEqualTo(2)
        assertThat(listOfFields[0]).isEqualTo(insertField1)
        assertThat(listOfFields[1]).isEqualTo(insertField2)
    }
}
