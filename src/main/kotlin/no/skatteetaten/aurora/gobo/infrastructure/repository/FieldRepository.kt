package no.skatteetaten.aurora.gobo.infrastructure.repository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

private val logger = KotlinLogging.logger {}

@Repository
class FieldRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun save(field: FieldDto) {
        val updated = namedParameterJdbcTemplate.update(
            "insert into field(name, count) values (:name, :count)",
            mapOf("name" to field.name, "count" to field.count)
        )
        logger.debug("Inserted field name:${field.name} count:${field.count}, rows updated $updated")
    }

    fun findAll(): List<FieldDto> {
        val resultSetExtrator = JdbcTemplateMapperFactory
            .newInstance()
            .addKeys("name", "count")
            .newResultSetExtractor(FieldDto::class.java)

        return namedParameterJdbcTemplate.query(
            "select f.name as name, f.count as count, fc.name, fc.count from field f left outer join field_client fc on f.name = fc.name",
            resultSetExtrator
        ) ?: emptyList()
    }

    fun findByName(name: String) = try {
        namedParameterJdbcTemplate.queryForObject<FieldDto>(
            "select name, count from field where name = :name",
            mapOf("name" to name)
        ) { rs, _ ->
            FieldDto(rs.getName(), rs.getCount())
        }
    } catch (e: EmptyResultDataAccessException) {
        null
    }

    fun incrementCounter(name: String, count: Long): Int =
        namedParameterJdbcTemplate.update(
            "update field set count = count + :count where name = :name",
            mapOf("count" to count, "name" to name)
        )

    private fun ResultSet.getName() = getString("name")
    private fun ResultSet.getCount() = getLong("count")
}
