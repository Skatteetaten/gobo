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
internal class FieldServiceImpl(val fieldRepo: FieldRepository) : FieldService {

    override fun addField(field: FieldDto): FieldDto {
        return fieldRepo.save(FieldEnity.fromDto(field)).toDto()
    }
    override fun getAllFields(): List<FieldDto> {
        return fieldRepo.findAll().map { it.toDto() }
    }

    override fun insertOrUpdateField(field: FieldDto): FieldDto {
        val foundField = fieldRepo.findById(field.name)
        return if (!foundField.isEmpty)
            fieldRepo.save(FieldEnity.fromDto(field, foundField.get())).toDto()
        else
            fieldRepo.save(FieldEnity.fromDto(field)).toDto()
    }

    override fun getFieldWithName(name: String): FieldDto? {
        return (fieldRepo.findById(name)).get().toDto()
    }
}
