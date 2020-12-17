package no.skatteetaten.aurora.gobo.infrastructure.field

data class Field(
    val name: String,
    val count: Long,
    val clients: List<FieldClient> = emptyList()
)

data class FieldClient(
    val name: String,
    val count: Long
)
