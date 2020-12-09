package no.skatteetaten.aurora.gobo.infrastructure.field

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger {}

@Profile("local")
@Service
class FieldServiceInMemory : FieldService {
    private val fields = LinkedMultiValueMap<String, FieldDto>()

    @PostConstruct
    fun init() {
        logger.info("Starting field service in-memory integration")
    }

    override fun addField(field: FieldDto) {
        logger.trace("Adding field:$field")
        fields.add(field.name, field)
    }

    override fun getFieldWithName(name: String) =
        fields[name]?.let { f ->
            val allClients = mutableMapOf<String, Long>()
            f.flatMap { it.clients }.forEach {
                allClients.computeIfPresent(it.name) { _, v ->
                    it.count + v
                } ?: allClients.put(it.name, it.count)
            }

            FieldDto(
                name = name,
                count = f.sumOf { it.count },
                clients = allClients.map { FieldClientDto(it.key, it.value) }
            )
        }

    override fun getAllFields() = fields.keys.map { getFieldWithName(it)!! }

    override fun insertOrUpdateField(field: FieldDto) {
        addField(field)
    }
}
