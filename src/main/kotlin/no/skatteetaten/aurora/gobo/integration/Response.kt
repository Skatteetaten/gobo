package no.skatteetaten.aurora.gobo.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Response<Item>(
    val success: Boolean = true,
    val message: String = "OK",
    val items: List<Item> = emptyList(),
    val errors: List<Item>? = null,
    val count: Int = items.size
) {
    constructor(item: Item) : this(items = listOf(item))
}
