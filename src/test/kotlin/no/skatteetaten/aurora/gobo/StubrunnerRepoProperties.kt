package no.skatteetaten.aurora.gobo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.w3c.dom.Document

const val stubrunnerUsername = "stubrunner.username"
const val stubrunnerPassword = "stubrunner.password"
const val stubrunnerRepoUrl = "stubrunner.repositoryRoot"

abstract class StrubrunnerRepoPropertiesEnabler {
    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun stubrunnerProperties(registry: DynamicPropertyRegistry) {
            StubrunnerRepoProperties(registry).populate()
        }
    }
}

class StubrunnerRepoProperties(private val registry: DynamicPropertyRegistry) {

    fun populate(
        jenkinsNexusJson: String = "/var/lib/jenkins/.custom/nexus/nexus.json",
        localMavenSettings: String = "${System.getProperty("user.home")}/.m2/settings.xml"
    ) {
        if (isJenkins()) {
            val document = jacksonObjectMapper().readTree(File(jenkinsNexusJson))
            registry.add(stubrunnerUsername) { document.get("username").textValue() }
            registry.add(stubrunnerPassword) { document.get("password").textValue() }
            registry.add(stubrunnerRepoUrl) {
                val url = document.get("nexusUrl").textValue()
                "$url/repository/maven-intern"
            }
        } else {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(localMavenSettings))
            registry.add(stubrunnerUsername) { document.xpath("/settings/servers/server/username") }
            registry.add(stubrunnerPassword) { document.xpath("/settings/servers/server/password") }
            registry.add(stubrunnerRepoUrl) { document.xpath("/settings/mirrors/mirror/url") }
        }
    }

    private fun isJenkins() =
        !System.getenv("CI").isNullOrEmpty() || !System.getenv("JENKINS_HOME").isNullOrEmpty()

    private fun Document.xpath(path: String) = XPathFactory.newInstance().newXPath().evaluate(path, this)
}
