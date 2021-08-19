package no.skatteetaten.aurora.gobo.infrastructure.field

import no.skatteetaten.aurora.gobo.infrastructure.field.repository.FieldClientRepository
import no.skatteetaten.aurora.gobo.infrastructure.field.repository.FieldRepository
import org.springframework.stereotype.Service

@Service
class FieldService(
    val fieldRepo: FieldRepository,
    private val fieldClientRepository: FieldClientRepository
) {

    fun addField(field: Field) {
        fieldRepo.save(field)
        fieldClientRepository.save(field.clients, field.name)
    }

    fun getAllFields() = fieldRepo.findAll()

    fun insertOrUpdateField(field: Field) {
        if (field.count > 0) {
            fieldRepo.incrementCounter(field.name, field.count).takeIfInsertRequired {
                fieldRepo.save(field)
            }
        }

        field.clients.forEach {
            if (it.count > 0) {
                fieldClientRepository.incrementCounter(it.name, field.name, it.count).takeIfInsertRequired {
                    fieldClientRepository.save(it, field.name)
                }
            }
        }
    }

    fun getFieldWithName(name: String) = fieldRepo.findWithName(name)

    fun getFieldCount() = fieldRepo.count()

    private fun Int.takeIfInsertRequired(fn: () -> Unit = {}) {
        if (this == 0) {
            fn()
        }
    }
}
