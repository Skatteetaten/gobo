package no.skatteetaten.aurora.gobo.infrastructure.repository

import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldEnity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional(Transactional.TxType.MANDATORY)
internal interface FieldRepository : JpaRepository<FieldEnity, Int> {

    fun findByName(@Param("name") name: String): FieldEnity?
}
