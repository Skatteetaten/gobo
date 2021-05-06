package no.skatteetaten.aurora.gobo.graphql.gobo

import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GoboQuery : Query {

    private val startTime = Instant.now()

    suspend fun gobo(dfe: DataFetchingEnvironment): Gobo {
        dfe.checkValidUserToken()
        return Gobo(startTime)
    }
}
