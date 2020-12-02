package no.skatteetaten.aurora.gobo.domain.model

data class FieldDto(
    val name: String,
    var count: Long,
    val clients: List<FieldClientDto> = emptyList()
)

data class FieldClientDto(
    val name: String,
    var count: Long
)
