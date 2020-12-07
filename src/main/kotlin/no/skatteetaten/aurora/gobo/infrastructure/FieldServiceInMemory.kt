package no.skatteetaten.aurora.gobo.infrastructure

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger {}

@Profile("local")
@Component
class FieldServiceInMemory : FieldService {
    private val fields = mutableSetOf<FieldDto>()

    @PostConstruct
    fun init() {
        logger.info("Starting field service in-memory integration")
    }

    override fun addField(field: FieldDto) {
        logger.info("Adding field:$field")
        fields.add(field)
    }

    override fun getFieldWithName(name: String) =
        fields.find { it.name == name }

    override fun getAllFields() = fields.toList()

    override fun insertOrUpdateField(field: FieldDto) {
        logger.info("Updating field:$field")
        getFieldWithName(field.name)?.let { f ->
            fields.remove(f)
            fields.add(field)
        } ?: addField(field)
    }
}
