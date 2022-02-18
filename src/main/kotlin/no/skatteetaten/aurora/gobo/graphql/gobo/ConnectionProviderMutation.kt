package no.skatteetaten.aurora.gobo.graphql.gobo

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.security.checkIsUserAuthorized
import org.springframework.stereotype.Component
import reactor.netty.resources.ConnectionProvider

private val logger = KotlinLogging.logger {}

@Component
class GoboMutation(val connectionProviders: List<ConnectionProvider>) : Mutation {

    suspend fun reset(dfe: DataFetchingEnvironment): String {
        dfe.checkIsUserAuthorized("APP_AUP_utv")
        logger.info("Resetting connection providers, size: ${connectionProviders.size}")
        connectionProviders.forEach {
            it.disposeLater().block()
        }
        return "Connection providers reset"
    }
}
