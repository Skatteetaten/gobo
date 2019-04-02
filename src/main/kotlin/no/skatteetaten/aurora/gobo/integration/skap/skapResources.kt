package no.skatteetaten.aurora.gobo.integration.skap

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CertificateResource(
    val id: String,
    val dn: String,
    val issuedDate: Instant?,
    val revokedDate: Instant?,
    val expiresDate: Instant?
)

data class WebsealState(
    val acl: Acl,
    val junctions: List<Junction>,
    val name: String,
    val namespace: String,
    val routeName: String
)

data class Junction(
    @JsonProperty("Active worker threads") val activeWorkerThreads: String,
    @JsonProperty("Allow Windows-style URLs") val allowWindowsStyleURLs: String,
    @JsonProperty("Authentication HTTP header") val authenticationHTTPheader: String,
    @JsonProperty("Basic authentication mode") val basicAuthenticationMode: String,
    @JsonProperty("Boolean Rule Header") val booleanRuleHeader: String,
    @JsonProperty("Case insensitive URLs") val caseInsensitiveURLs: String,
    @JsonProperty("Current requests") val currentRequests: String,
    @JsonProperty("Delegation support") val delegationSupport: String,
    @JsonProperty("Forms based SSO") val formsBasedSSO: String,
    @JsonProperty("Hostname") val hostname: String,
    @JsonProperty("ID") val id: String,
    @JsonProperty("Insert WebSEAL session cookies") val insertWebSEALSessionCookies: String,
    @JsonProperty("Insert WebSphere LTPA cookies") val insertWebSphereLTPACookies: String,
    @JsonProperty("Junction hard limit") val junctionHardLimit: String,
    @JsonProperty("Junction soft limit") val junctionSoftLimit: String,
    @JsonProperty("Mutually authenticated") val mutuallyAuthenticated: String,
    @JsonProperty("Operational State") val operationalState: String,
    @JsonProperty("Port") val port: String,
    @JsonProperty("Query-contents") val queryContents: String,
    @JsonProperty("Query_contents URL") val queryContentsURL: String,
    @JsonProperty("Remote Address HTTP header") val remoteAddressHTTPHeader: String,
    @JsonProperty("Request Encoding") val requestEncoding: String,
    @JsonProperty("Server 1") val server1: String,
    @JsonProperty("Server DN") val serverDN: String,
    @JsonProperty("Server State") val serverState: String,
    @JsonProperty("Stateful junction") val statefulJunction: String,
    @JsonProperty("TFIM junction SSO") val tfimjunctionSSO: String,
    @JsonProperty("Total requests") val totalRequests: String,
    @JsonProperty("Type") val type: String,
    @JsonProperty("Virtual Host Junction label") val virtualHostJunctionLabel: String,
    @JsonProperty("Virtual hostname") val virtualHostname: String,
    @JsonProperty("local IP address") val localIPAddress: String
)

data class Acl(
    val aclName: String,
    val anyOther: Boolean,
    val `open`: Boolean,
    val roles: List<String>
)
