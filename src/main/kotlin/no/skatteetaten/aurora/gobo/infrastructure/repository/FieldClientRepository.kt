package no.skatteetaten.aurora.gobo.infrastructure.repository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

private val logger = KotlinLogging.logger {}

@Repository
class FieldClientRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun save(client: List<FieldClientDto>, fieldName: String) {
        client.forEach { save(it, fieldName) }
    }

    fun save(client: FieldClientDto, fieldName: String) {
        val sql = "insert into field_client(name, count, field_name) values (:name, :count, :fieldName)"
        val updated = namedParameterJdbcTemplate.update(
            sql,
            mapOf("name" to client.name, "count" to client.count, "fieldName" to fieldName)
        )
        logger.debug("Inserted field_client name:${client.name} count:${client.count} fieldName:$fieldName, rows updated $updated")
    }

    fun findByFieldName(fieldName: String): List<FieldClientDto> {
        val sql = "select name, count from field_client where field_name = :field_name"
        return namedParameterJdbcTemplate.query<FieldClientDto>(sql, mapOf("field_name" to fieldName)) { rs, _ ->
            FieldClientDto(rs.getName(), rs.getCount())
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
