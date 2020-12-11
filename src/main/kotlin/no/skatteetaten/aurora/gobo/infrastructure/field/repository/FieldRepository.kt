package no.skatteetaten.aurora.gobo.infrastructure.field.repository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldClient
import no.skatteetaten.aurora.gobo.infrastructure.field.Field
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.util.LinkedMultiValueMap
import java.sql.ResultSet

private val logger = KotlinLogging.logger {}

@Repository
class FieldRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    private val resultSetExtractor = ResultSetExtractor<List<Field>> {
        val fields = mutableSetOf<Field>()
        val fieldClients = LinkedMultiValueMap<String, FieldClient>()
        while (it.next()) {
            val name = it.getName()
            fields.addField(name, it.getCount())

            it.getClientName()?.let { clientName ->
                fieldClients.addClient(name, clientName, it.getClientCount())
            }
        }

        fields.map { field ->
            fieldClients[field.name]?.let { clients ->
                field.copy(clients = clients)
            } ?: field
        }
    }

    fun save(field: Field) {
        val sql = "insert into field(name, count) values (:name, :count)"
        val updated = namedParameterJdbcTemplate.update(sql, mapOf("name" to field.name, "count" to field.count))
        logger.debug("Inserted field name:${field.name} count:${field.count}, rows updated $updated")
    }

    fun findAll(): List<Field> {
        val sql =
            "select f.name as name, f.count as count, fc.name as client_name, fc.count as client_count from field f left join field_client fc on f.name = fc.field_name"
        return namedParameterJdbcTemplate.query(sql, resultSetExtractor) ?: emptyList()
    }

    fun findByName(name: String): Field? {
        val sql =
            "select f.name as name, f.count as count, fc.name as client_name, fc.count as client_count from field f left join field_client fc on f.name = fc.field_name where f.name = :name"
        return namedParameterJdbcTemplate.query(sql, mapOf("name" to name), resultSetExtractor)?.ifEmpty { null }
            ?.first()
    }

    fun incrementCounter(name: String, count: Long): Int {
        val sql = "update field set count = count + :count where name = :name"
        return namedParameterJdbcTemplate.update(sql, mapOf("count" to count, "name" to name)).also {
            logger.debug("Incremented field counter name:$name counter:$count")
        }
    }

    private fun MutableSet<Field>.addField(name: String, count: Long) = add(Field(name, count))
    private fun LinkedMultiValueMap<String, FieldClient>.addClient(fieldName: String, name: String, count: Long) =
        add(fieldName, FieldClient(name, count))

    private fun ResultSet.getName() = getString("name")
    private fun ResultSet.getClientName(): String? = getString("client_name")
    private fun ResultSet.getCount() = getLong("count")
    private fun ResultSet.getClientCount() = getLong("client_count")
}
