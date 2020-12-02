package no.skatteetaten.aurora.gobo.infrastructure.entity

import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.FetchType
import javax.persistence.CascadeType

@Entity
@Table(name = "field")
data class FieldEnity(
    @Id val name: String,

    val count: Long,

    @OneToMany(mappedBy = "field", cascade = arrayOf(CascadeType.ALL), fetch = FetchType.EAGER)
    var fieldclients: List<FieldClientEnity> = emptyList()

) {

    private constructor() : this("", 0)

    fun toDto(): FieldDto = FieldDto(
        name = this.name,
        count = this.count,
        clients = fieldclients.map { FieldClientDto(it.name, it.count) }
    )

    companion object {
        fun fromDto(dto: FieldDto) = FieldEnity(
            name = dto.name,
            count = dto.count
        )

        fun fromDto(updateField: FieldDto, currentField: FieldEnity) = FieldEnity(
            currentField.name,
            updateField.count
        )
    }
}
