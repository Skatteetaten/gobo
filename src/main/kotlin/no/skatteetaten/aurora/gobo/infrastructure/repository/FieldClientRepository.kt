package no.skatteetaten.aurora.gobo.infrastructure.repository

import no.skatteetaten.aurora.gobo.infrastructure.entity.FieldClientEnity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional(Transactional.TxType.MANDATORY)
interface FieldClientRepository : JpaRepository<FieldClientEnity, String>
