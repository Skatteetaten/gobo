package no.skatteetaten.aurora.gobo

import assertk.Assert
import assertk.assertThat
import assertk.assertions.support.expected
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import java.util.function.Supplier

class TestDynamicPropertyRegistry : DynamicPropertyRegistry {
    val properties: MutableMap<String, String> = mutableMapOf()

    override fun add(name: String, value: Supplier<Any>) {
        properties[name] = value.get() as String
    }
}

class StubrunnerRepoPropertiesTest {

    @Test
    fun `Populate stubrunner properties from local config`() {
        val registry = TestDynamicPropertyRegistry()
        StubrunnerRepoProperties(registry).populate()

        assertThat(registry).hasStubrunnerProperties()
    }

    @Test
    fun `Populate stubrunner properties from jenkins`() {
        val env = mockk<SystemEnv>()
        every { env.get("CI") } returns "true"

        val registry = TestDynamicPropertyRegistry()
        StubrunnerRepoProperties(registry, env).populate(jenkinsNexusJson = "src/test/resources/nexus.json")

        assertThat(registry).hasStubrunnerProperties()
    }

    private fun Assert<TestDynamicPropertyRegistry>.hasStubrunnerProperties() = given { actual ->
        val username = actual.properties[stubrunnerUsername]
        val password = actual.properties[stubrunnerPassword]
        val repoUrl = actual.properties[stubrunnerRepoUrl]
        if (actual.properties.size == 3 && username != null && password != null && repoUrl != null) return
        expected("stubrunner properties to be set, username:$username password:$password repoUrl:$repoUrl")
    }
}
