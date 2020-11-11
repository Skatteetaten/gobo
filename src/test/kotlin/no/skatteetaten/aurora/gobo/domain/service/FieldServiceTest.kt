package no.skatteetaten.aurora.gobo.domain.service

import no.skatteetaten.aurora.gobo.FieldConfiguration
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldRepository
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import org.assertj.core.api.JUnitSoftAssertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = arrayOf(
        FieldConfiguration::class
    )
)
@DataJpaTest
internal class FieldServiceTest {

    @Autowired
    lateinit var service: FieldService
    @Autowired
    lateinit var repository: FieldRepository

    @get:Rule
    var softly = JUnitSoftAssertions()

    @Test
    fun `'addField' should return created entity`() {
        val (name, count) = service.addField(FieldDto(name = "gobo.usage.usedFields.name", count = 10))
        softly.assertThat(name).isEqualTo("gobo.usage.usedFields.name")
        softly.assertThat(count).isEqualTo(10)
    }

    @Test
    fun `'insertOrUpdateField' first addField and when update field should return created entity`() {
        var insertField = service.addField(FieldDto(name = "gobo.usage.usedFields.name", count = 10))
        softly.assertThat(insertField.name).isEqualTo("gobo.usage.usedFields.name")
        softly.assertThat(insertField.count).isEqualTo(10)

        insertField.count = 12
        val (name, count) = service.insertOrUpdateField(insertField)
        softly.assertThat(name).isEqualTo("gobo.usage.usedFields.name")
        softly.assertThat(count).isEqualTo(12)
    }

    @Test
    fun `'insertOrUpdateField' two times insertOrUpdateField should return created entity`() {
        var tmpField = FieldDto(name = "gobo.usage.usedFields.name", count = 10)
        var insertField = service.insertOrUpdateField(tmpField)
        softly.assertThat(insertField.name).isEqualTo("gobo.usage.usedFields.name")
        softly.assertThat(insertField.count).isEqualTo(10)

        insertField.count = 12
        val (name, count) = service.insertOrUpdateField(insertField)
        softly.assertThat(name).isEqualTo("gobo.usage.usedFields.name")
        softly.assertThat(count).isEqualTo(12)
    }

    @Test
    fun `'getAllFields' should return list of created entity`() {
        var tmpField1 = FieldDto(name = "gobo.usage.usedFields", count = 42)
        var insertField1 = service.insertOrUpdateField(tmpField1)

        var tmpField2 = FieldDto(name = "gobo.usage.usedFields.name", count = 40)
        var insertField2 = service.insertOrUpdateField(tmpField2)

        val listOfFields = service.getAllFields()
        softly.assertThat(listOfFields.size).isEqualTo(2)
        softly.assertThat(listOfFields.get(0)).isEqualTo(insertField1)
        softly.assertThat(listOfFields.get(1)).isEqualTo(insertField2)
    }
}
