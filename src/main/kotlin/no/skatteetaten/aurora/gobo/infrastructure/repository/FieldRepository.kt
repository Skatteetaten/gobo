package no.skatteetaten.aurora.gobo.infrastructure.repository

import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldEnity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional(Transactional.TxType.MANDATORY)
interface FieldRepository : JpaRepository<FieldEnity, String> {

    fun findByName(@Param("name") name: String): FieldEnity?

    @Modifying(clearAutomatically = true)
    @Query("update FieldEnity f set f.count = f.count + ?1 where f.name = ?2")
    fun incrementCounter(counter: Long, name: String): Int
}
