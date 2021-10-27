package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
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
        localMavenSettings: String = "${System.getProperty("user.home")}/.m2/settings.xml"
    ) {
        if (isJenkins()) {
            logger.info("Reading stubrunner properties from nexus config in jenkins")
            val document = jacksonObjectMapper().readTree(File(jenkinsNexusJson))
            registry.add(stubrunnerUsername) { document.get("username").textValue() }
            registry.add(stubrunnerPassword) { document.get("password").textValue() }
            registry.add(stubrunnerRepoUrl) {
                val url = document.get("nexusUrl").textValue()
                "$url/repository/maven-intern"
            }
        } else {
            logger.info("Reading stubrunner properties from maven settings.xml")
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(localMavenSettings))
            val credentialsQuery = "/settings/servers/server/id[contains(text(), 'nexus')]/following-sibling::"
            registry.add(stubrunnerUsername) { document.xpath(credentialsQuery + "username") }
            registry.add(stubrunnerPassword) { document.xpath(credentialsQuery + "password") }
            registry.add(stubrunnerRepoUrl) { document.xpath("/settings/mirrors/mirror/id[contains(text(), 'nexus')]/following-sibling::url") }
        }
    }

    private fun isJenkins() =
        !System.getenv("CI").isNullOrEmpty() || !System.getenv("JENKINS_HOME").isNullOrEmpty()

    private fun Document.xpath(path: String) = XPathFactory.newInstance().newXPath().evaluate(path, this)
}
