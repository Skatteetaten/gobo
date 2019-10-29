package no.skatteetaten.aurora.gobo.integration.mokey

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URL
import java.nio.charset.Charset
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.Link
import org.springframework.web.util.UriUtils
import uk.q3c.rest.hal.HalLink
import uk.q3c.rest.hal.HalResource
import uk.q3c.rest.hal.Links

fun HalResource.findLink(rel: String): String {
    return _links.link(rel).let {
        UriUtils.decode(it.href, Charset.defaultCharset())
    }
}

fun HalResource.addAll(links: Links) =
    links.toGoboLinks().forEach {
        this.link(it.name, HalLink(it.url.toString()))
    }

fun HalResource.linkHref(propertyName: String) =
    link(propertyName)?.href ?: throw IllegalArgumentException("Link with name $propertyName not found")

fun HalResource.linkHrefs(vararg propertyNames: String) =
    propertyNames.map { linkHref(it) }

fun Links.toGoboLinks(): List<Link> {
    val values = jacksonObjectMapper().valueToTree<JsonNode>(this)
    val links = mutableListOf<Link>()
    values.fields().forEach {
        val name = it.key
        val url = it.value.at("/href").asText()
        if (!url.isNullOrEmpty()) {
            links.add(Link(name, URL(url)))
        }
    }
    return links
}
