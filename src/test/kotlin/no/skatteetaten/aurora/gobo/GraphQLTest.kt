package no.skatteetaten.aurora.gobo

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.springframework.boot.test.context.SpringBootTest

@Tags(
    Tag("graphql"),
    Tag("spring")
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["management.server.port=-1"])
annotation class GraphQLTest
