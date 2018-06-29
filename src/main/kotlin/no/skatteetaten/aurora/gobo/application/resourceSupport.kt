package no.skatteetaten.aurora.gobo.application

import org.springframework.hateoas.Link
import org.springframework.hateoas.ResourceSupport

fun ResourceSupport.applicationInstanceDetailsId(): String? {
    val link = this.getLink("ApplicationInstanceDetails") ?: this.getLink(Link.REL_SELF)
    return link?.href?.substringAfterLast("/")
}
