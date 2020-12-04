package no.skatteetaten.aurora.gobo.infrastructure.repository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

private val logger = KotlinLogging.logger {}

@Repository
class FieldClientRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun save(client: FieldClientDto, fieldName: String) {
        val updated = namedParameterJdbcTemplate.update(
            "insert into field_client(name, count, field_name) values (:name, :count, :fieldName)",
            mapOf("name" to client.name, "count" to client.count, "fieldName" to fieldName)
        )
        logger.debug("Inserted field name:${client.name} count:${client.count} fieldName:$fieldName, rows updated $updated")
    }

    fun findByFieldName(fieldName: String): List<FieldClientDto> =
        namedParameterJdbcTemplate.query<FieldClientDto>(
            "select name, count from field_client where field_name = :field_name",
            mapOf("field_name" to fieldName)
        ) { rs, _ ->
            FieldClientDto(rs.getName(), rs.getCount())
        }

    fun findByNameAndFieldName(name: String, fieldName: String): List<FieldClientDto> =
        namedParameterJdbcTemplate.query<FieldClientDto>(
            "select name, count from field_client where name = :name and field_name = :field_name",
            mapOf("name" to name, "field_name" to fieldName)
        ) { rs, _ ->
            FieldClientDto(rs.getName(), rs.getCount())
        }

    fun incrementCounter(name: String, count: Long): Int =
        namedParameterJdbcTemplate.update(
            "update field_client set count = count + :count where name = :name",
            mapOf("count" to count, "name" to name)
        )

    private fun ResultSet.getName() = getString("name")
    private fun ResultSet.getCount() = getLong("count")
}
