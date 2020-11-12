package no.skatteetaten.aurora.gobo.infrastructure.entity

import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "FIELD")
internal data class FieldEnity(
    @Id val name: String? = null,
    val count: Int? = null
) {

    public constructor() : this("", 0)

    fun toDto(): FieldDto = FieldDto(
        name = this.name!!,
        count = this.count
    )

    companion object {
        fun fromDto(dto: FieldDto) = FieldEnity(
            name = dto.name,
            count = dto.count
        )

        fun fromDto(updateField: FieldDto, currentField: FieldEnity) = FieldEnity(
            name = currentField.name!!,
            count = updateField.count ?: currentField.count
        )
    }
}