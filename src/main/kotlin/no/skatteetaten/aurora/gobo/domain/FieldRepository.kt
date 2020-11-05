package no.skatteetaten.aurora.gobo.domain

import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldEnity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional(Transactional.TxType.MANDATORY)
internal interface FieldRepository : JpaRepository<FieldEnity, String>
