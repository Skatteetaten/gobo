package no.skatteetaten.aurora.gobo.infrastructure

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldEnity
import no.skatteetaten.aurora.gobo.infrastructure.repository.FieldRepository
import org.springframework.stereotype.Service
import javax.transaction.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class FieldServiceImpl(val fieldRepo: FieldRepository) : FieldService {

    override fun addField(field: FieldDto): FieldDto {
        return fieldRepo.save(FieldEnity.fromDto(field)).toDto()
    }

    override fun getAllFields(): List<FieldDto> {
        var res = fieldRepo.findAll()
//        return fieldRepo.findAll().map { it.toDto() }
        return emptyList()
    }

    override fun insertOrUpdateField(field: FieldDto) {
        fieldRepo.findByName(field.name)?.let {
            val updated = fieldRepo.incrementCounter(field.count, field.name)
            if (updated != 1) {
                logger.error("Unable to update name:${field.name} with count:${field.count}, rows updated:$updated")
            }
        } ?: fieldRepo.save(FieldEnity.fromDto(field)).also {
            logger.debug("Saved field with name:${it.name} and count:${field.count}")
        }
        fieldRepo.flush()
    }

    override fun getFieldWithName(name: String): FieldDto? {
        return fieldRepo.findById(name).takeIf { it.isPresent }?.get()?.toDto()
    }
}
