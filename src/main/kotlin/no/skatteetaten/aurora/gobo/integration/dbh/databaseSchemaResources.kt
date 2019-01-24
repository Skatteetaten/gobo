package no.skatteetaten.aurora.gobo.integration.dbh

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseInstanceResource(val engine: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseUserResource(val username: String, val password: String, val type: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseMetadataResource(val sizeInMb: Double)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseSchemaResource(
    val id: String,
    val type: String,
    val jdbcUrl: String,
    val name: String,
    val createdDate: Long,
    val lastUsedDate: Long?,
    val databaseInstance: DatabaseInstanceResource,
    val users: List<DatabaseUserResource>,
    val metadata: DatabaseMetadataResource,
    val labels: Map<String, String>
) {
    val affiliation: String
        get() = labels.filter { it.key == "affiliation" }.map { it.value }.first()

    val createdBy: String
        get() = labels.filter { it.key == "userId" }.map { it.value }.first()

    val discriminator: String
        get() = labels.filter { it.key == "name" }.map { it.value }.first()

    val description: String?
        get() = labels.filter { it.key == "description" }.map { it.value }.firstOrNull()

    fun createdDateAsInstant(): Instant = Instant.ofEpochMilli(createdDate)

    fun lastUsedDateAsInstant(): Instant? =
        lastUsedDate?.let {
            return Instant.ofEpochMilli(it)
        }

    fun containsRequiredLabels() =
        labels.containsKey("affiliation") && labels.containsKey("userId") && labels.containsKey("name")
}
