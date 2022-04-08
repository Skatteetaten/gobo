package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.w3c.dom.Document
import reactor.core.publisher.Hooks
import reactor.core.scheduler.Schedulers
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

const val stubrunnerUsername = "stubrunner.username"
const val stubrunnerPassword = "stubrunner.password"
const val stubrunnerRepoUrl = "stubrunner.repositoryRoot"

abstract class StrubrunnerRepoPropertiesEnabler {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun objectMapper() = jacksonObjectMapper()
    }

    // Spring cloud sleuth does not clean up its hooks when the spring context shuts down
    // https://github.com/spring-cloud/spring-cloud-sleuth/issues/1712
    @BeforeEach
    fun setUp() {
        Hooks.resetOnEachOperator()
        Hooks.resetOnLastOperator()
        Schedulers.resetOnScheduleHooks()
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun stubrunnerProperties(registry: DynamicPropertyRegistry) {
            StubrunnerRepoProperties(registry).populate()
        }
    }
}

private val logger = KotlinLogging.logger {}

class StubrunnerRepoProperties(private val registry: DynamicPropertyRegistry) {

    fun populate(
        jenkinsNexusJson: String = "/var/lib/jenkins/.custom/nexus/nexus.json",
        localGradleSettings: String = "${System.getProperty("user.home")}/.gradle/init.gradle",
        localMavenSettings: String = "${System.getProperty("user.home")}/.m2/settings.xml",
    ) {
        when {
            isJenkins() -> jacksonObjectMapper().readTree(File(jenkinsNexusJson)).let {
                logger.info("Reading stubrunner properties from nexus config in jenkins")
                registry.add(stubrunnerUsername) { it.get("username").textValue() }
                registry.add(stubrunnerPassword) { it.get("password").textValue() }
                registry.add(stubrunnerRepoUrl) {
                    val url = it.get("nexusUrl").textValue()
                    "$url/repository/maven-intern"
                }
            }
            File(localGradleSettings).isFile -> File(localGradleSettings).readText().let {
                logger.info("Reading stubrunner properties from init.gradle")

                val nexusVariabelUser = "def nexus3User"
                val nexusVariabelPass = "def nexus3Password"
                if (it.contains(nexusVariabelUser)) {
                    registry.add(stubrunnerUsername) { it.substringAfter("$nexusVariabelUser = ").removeAfterNewLine() }
                    registry.add(stubrunnerPassword) { it.substringAfter("$nexusVariabelPass = ").removeAfterNewLine() }
                } else {
                    registry.add(stubrunnerUsername) { it.substringAfter("username").removeAfterNewLine() }
                    registry.add(stubrunnerPassword) { it.substringAfter("password").removeAfterNewLine() }
                }

                registry.add(stubrunnerRepoUrl) { it.substringAfter("url").removeAfterNewLine() }
            }
            else -> DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(localMavenSettings)).let {
                logger.info("Reading stubrunner properties from maven settings.xml")
                val credentialsQuery = "/settings/servers/server/id[contains(text(), 'nexus')]/following-sibling::"
                registry.add(stubrunnerUsername) { it.xpath(credentialsQuery + "username") }
                registry.add(stubrunnerPassword) { it.xpath(credentialsQuery + "password") }
                registry.add(stubrunnerRepoUrl) { it.xpath("/settings/mirrors/mirror/id[contains(text(), 'nexus')]/following-sibling::url") }
            }
        }
    }

    private fun String.removeAfterNewLine() = split("\n").first().trim().removeSurrounding("\"")

    private fun isJenkins() =
        !System.getenv("CI").isNullOrEmpty() || !System.getenv("JENKINS_HOME").isNullOrEmpty()

    private fun Document.xpath(path: String) = XPathFactory.newInstance().newXPath().evaluate(path, this)
}
