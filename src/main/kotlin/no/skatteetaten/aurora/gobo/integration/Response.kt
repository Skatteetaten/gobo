package no.skatteetaten.aurora.gobo.integration

data class Response<Item>(
    val success: Boolean = true,
    val message: String = "OK",
    val items: List<Item>,
    val count: Int = items.size
)
