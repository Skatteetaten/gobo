package no.skatteetaten.aurora.gobo.domain.model

data class FieldDto(
    var id: Int,
    var name: String,
    var count: Int? = null
)

// data class FieldClientDto(
//        val fieldName: String,
//        val name: String,
//        var count: Int? = null
// )
