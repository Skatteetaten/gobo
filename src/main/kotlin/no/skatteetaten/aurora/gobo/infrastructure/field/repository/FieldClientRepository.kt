package no.skatteetaten.aurora.gobo.infrastructure.field.repository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.infrastructure.ConditionalOnDatabaseUrl
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldClient
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

private val logger = KotlinLogging.logger {}

@ConditionalOnDatabaseUrl
@Repository
class FieldClientRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun save(client: List<FieldClient>, fieldName: String) {
        client.forEach { save(it, fieldName) }
    }

    fun save(client: FieldClient, fieldName: String) {
        val sql = "insert into field_client(name, count, field_name) values (:name, :count, :fieldName)"
        val updated = namedParameterJdbcTemplate.update(
            sql,
            mapOf("name" to client.name, "count" to client.count, "fieldName" to fieldName)
        )
        logger.debug("Inserted field_client name:${client.name} count:${client.count} fieldName:$fieldName, rows updated $updated")
    }

    fun findByFieldName(fieldName: String): List<FieldClient> {
        val sql = "select name, count from field_client where field_name = :field_name"
        return namedParameterJdbcTemplate.query(sql, mapOf("field_name" to fieldName)) { rs, _ ->
            FieldClient(rs.getName(), rs.getCount())
        }
    }

    fun incrementCounter(name: String, fieldName: String, count: Long): Int {
        val sql = "update field_client set count = count + :count where name = :name and field_name = :field_name"
        return namedParameterJdbcTemplate.update(
            sql,
            mapOf("count" to count, "name" to name, "field_name" to fieldName)
        ).also {
            logger.debug("Updated field client counter name:$name fieldName:$fieldName counter:$count")
        }
    }

    private fun ResultSet.getName() = getString("name")
    private fun ResultSet.getCount() = getLong("count")
}
