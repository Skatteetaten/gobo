package no.skatteetaten.aurora.gobo.infrastructure.entity

import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "FIELD")
internal data class FieldEnity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    val name: String,
    val count: Int? = null
) {

    private constructor() : this(0, "", 0)

    fun toDto(): FieldDto = FieldDto(
        id = this.id,
        name = this.name,
        count = this.count
    )

    companion object {
        fun fromDto(dto: FieldDto) = FieldEnity(
            id = dto.id ?: 0,
            name = dto.name,
            count = dto.count
        )

        fun fromDto(updateField: FieldDto, currentField: FieldEnity) = FieldEnity(
            name = currentField.name!!,
            count = updateField.count ?: currentField.count
        )
    }
}
