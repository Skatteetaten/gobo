package no.skatteetaten.aurora.gobo.domain

import no.skatteetaten.aurora.gobo.domain.model.FieldDto

interface FieldService {
    fun addField(city: FieldDto): FieldDto
}
