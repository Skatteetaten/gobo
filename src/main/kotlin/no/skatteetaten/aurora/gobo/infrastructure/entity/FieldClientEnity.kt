package no.skatteetaten.aurora.gobo.infrastructure.entity

// import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.FetchType
import javax.persistence.JoinColumn

@Entity
@Table(name = "field_client")
data class FieldClientEnity(
    @Id
    val name: String,

    val count: Long,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "field_name")
    var field: FieldEnity
) {

    private constructor() : this("", 0, FieldEnity("", 0))

    override fun toString(): String {
        return "{name: $name, count: $count}"
    }
}
