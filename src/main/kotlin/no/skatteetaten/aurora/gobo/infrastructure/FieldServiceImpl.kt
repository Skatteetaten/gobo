package no.skatteetaten.aurora.gobo.infrastructure

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldClientRepository
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldRepository
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class FieldServiceImpl(
    val fieldRepo: FieldRepository,
    val fieldClientRepository: FieldClientRepository
) : FieldService {

    override fun addField(field: FieldDto) {
        fieldRepo.save(field)
        fieldClientRepository.save(field.clients, field.name)
    }

    override fun getAllFields(): List<FieldDto> {
        return fieldRepo.findAll().map {
            val clients = fieldClientRepository.findByFieldName(it.name)
            it.copy(clients = clients)
        }
    }

    override fun insertOrUpdateField(field: FieldDto) {
        fieldRepo.incrementCounter(field.name, field.count).takeIfInsertRequired {
            fieldRepo.save(field)
        }

        field.clients.forEach {
            fieldClientRepository.incrementCounter(it.name, field.name, it.count).takeIfInsertRequired {
                fieldClientRepository.save(it, field.name)
            }
        }
    }

    override fun getFieldWithName(name: String) =
        fieldRepo.findByName(name)?.let {
            it.copy(clients = fieldClientRepository.findByFieldName(it.name))
        }

    private fun Int.takeIfInsertRequired(fn: () -> Unit = {}) {
        if (this == 0) {
            fn()
        }
    }
}
