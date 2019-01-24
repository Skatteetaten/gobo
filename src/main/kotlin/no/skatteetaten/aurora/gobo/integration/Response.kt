package no.skatteetaten.aurora.gobo.integration

data class Response<T>(
    val success: Boolean = true,
    val message: String = "OK",
    val items: List<T>,
    val count: Int = items.size
)
