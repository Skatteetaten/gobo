package no.skatteetaten.aurora.gobo.infrastructure.repository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.FieldRepository
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldEnity
import org.springframework.stereotype.Service
import javax.transaction.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional
internal class FieldServiceImpl(val fieldRepo: FieldRepository) : FieldService {

    override fun addField(field: FieldDto): FieldDto {
        logger.debug("Adding field: {}", field)
        return fieldRepo.save(FieldEnity.fromDto(field)).toDto()
    }
}
