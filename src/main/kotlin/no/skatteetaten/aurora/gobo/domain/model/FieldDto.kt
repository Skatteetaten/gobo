package no.skatteetaten.aurora.gobo.domain.model

data class FieldDto(
    var id: Int? = 0,
    var name: String,
    var count: Long
)

// data class FieldClientDto(
//        val fieldName: String,
//        val name: String,
//        var count: Int? = null
// )
