package no.skatteetaten.aurora.gobo.domain

import no.skatteetaten.aurora.gobo.domain.model.FieldDto

interface FieldService {
    fun addField(field: FieldDto): FieldDto

    fun getFieldWithName(name: String): FieldDto?

    fun getAllFields(): List<FieldDto>

    fun insertOrUpdateField(field: FieldDto): FieldDto
}
