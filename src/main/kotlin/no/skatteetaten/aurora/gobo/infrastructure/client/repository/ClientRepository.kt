package no.skatteetaten.aurora.gobo.infrastructure.client.repository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.domain.model.ClientDto
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

private val logger = KotlinLogging.logger {}

@Repository
class ClientRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun save(client: ClientDto) {
        val sql = "insert into client(name, count) values(:name, :count)"
        val updated = namedParameterJdbcTemplate.update(sql, mapOf("name" to client.name, "count" to client.count))
        logger.debug("Inserted field name:${client.name} count:${client.count}, rows updated $updated")
    }

    fun findAll(): List<ClientDto> {
        val sql = "select name, count from client"
        return namedParameterJdbcTemplate.query(sql) { rs, _ ->
            ClientDto(rs.getName(), rs.getCount())
        }
    }

    fun findByName(name: String): ClientDto? {
        val sql = "select name, count from client where name = :name"
        return namedParameterJdbcTemplate.query(sql, mapOf("name" to name)) { rs, _ ->
            ClientDto(rs.getName(), rs.getCount())
        }.ifEmpty { null }?.first()
    }

    fun incrementCounter(name: String, count: Long): Int {
        val sql = "update client set count = count + :count where name = :name"
        return namedParameterJdbcTemplate.update(sql, mapOf("count" to count, "name" to name)).also {
            logger.debug("Incremented client counter name:$name counter:$count")
        }
    }

    private fun ResultSet.getName() = this.getString("name")
    private fun ResultSet.getCount() = this.getLong("count")
}
