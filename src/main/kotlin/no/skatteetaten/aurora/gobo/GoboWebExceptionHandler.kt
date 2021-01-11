package no.skatteetaten.aurora.gobo

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class GoboWebExceptionHandler : ErrorWebExceptionHandler {
    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        exchange.response
        return Mono.empty()
    }
}
