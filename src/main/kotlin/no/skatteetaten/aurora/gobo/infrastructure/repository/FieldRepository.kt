package no.skatteetaten.aurora.gobo.infrastructure.repository

import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldEnity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*
import javax.transaction.Transactional

@Repository
@Transactional(Transactional.TxType.MANDATORY)
internal interface FieldRepository : JpaRepository<FieldEnity, Int> {

    @Query("FROM FIELD field WHERE field.name = :name")
    fun findByName(@Param("name") name: String): Optional<FieldEnity>;
}
