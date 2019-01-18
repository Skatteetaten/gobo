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
    val createdDate: Instant,
    val lastUsedDate: Instant?,
    val databaseInstance: DatabaseInstanceResource,
    val users: List<DatabaseUserResource>,
    val metadata: DatabaseMetadataResource,
    val labels: Map<String, String>
) {
    val affiliation: String
        get() = labels.filter { it.key == "affiliation" }.map { it.value }.first()

    val createdBy: String
        get() = labels.filter { it.key == "userId" }.map { it.value }.first()

    fun containsRequiredLabels() = labels.containsKey("affiliation") && labels.containsKey("userId")
}