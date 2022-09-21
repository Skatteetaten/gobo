import java.time.Duration
import io.gatling.javaapi.core.CoreDsl.ElFileBody
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.nothingFor
import io.gatling.javaapi.core.CoreDsl.rampUsers
import io.gatling.javaapi.core.CoreDsl.rampUsersPerSec
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http

class GoboLoadtestSimulation : Simulation() {

    val token = System.getenv("token")
    private val goboMockedUrl = "https://m78879-gobo-aup.apps.utv01.paas.skead.no/graphql"
    private val goboUrl = "https://gobo-aup.apps.utv01.paas.skead.no/graphql"

    private val deleteMokeyLatencyToxic = exec(
        http("mokeyDeleteLatencyToxicRequest")
            .post(goboUrl)
            .body(ElFileBody("mokey_delete_latency_toxic_mutation.json"))
    )

    private val deleteMokeyTimeoutToxic = exec(
        http("mokeyDeleteTimeoutToxicRequest")
            .post(goboUrl)
            .body(ElFileBody("mokey_delete_timeout_toxic_mutation.json"))
    )

    private val deleteMokeyDelayToxic = exec(
        http("mokeyDeleteDelayToxicRequest")
            .post(goboUrl)
            .body(ElFileBody("mokey_delete_delay_toxic_mutation.json"))
    )

    private val addMokeyLatencyToxic = exec(
        http("mokeyAddLatencyToxicRequest")
            .post(goboUrl)
            .body(ElFileBody("mokey_add_latency_toxic_mutation.json"))
            .check(jsonPath("$.errors").notExists())
    ).exitHereIfFailed()

    private val addMokeyTimoutToxic = exec(
        http("mokeyAddTimeoutToxicRequest")
            .post(goboUrl)
            .body(ElFileBody("mokey_add_timeout_toxic_mutation.json"))
            .check(jsonPath("$.errors").notExists())
    ).exitHereIfFailed()

    private val addMokeyDelayToxic = exec(
        http("mokeyAddDelayToxicRequest")
            .post(goboUrl)
            .body(ElFileBody("mokey_add_delay_toxic_mutation.json"))
            .check(jsonPath("$.errors").notExists())
    ).exitHereIfFailed()

    val deleteToxics = scenario("deleteToxics").exec(
        deleteMokeyLatencyToxic,
        deleteMokeyTimeoutToxic,
        deleteMokeyDelayToxic
    )

    val addToxics = scenario("addToxics").exec(
        addMokeyLatencyToxic,
        addMokeyTimoutToxic,
        // addMokeyDelayToxic
    )

    val databaseSchemaScenario = scenario("Affiliations").exec(
        http("databasSchemaRequest")
            .post(goboMockedUrl)
            .body(ElFileBody("databaseSchemas_query.json"))
            .check(jsonPath("$.errors").notExists())
    )

    val booberVaultScenario = scenario("Vault").exec(
        http("booberVaultRequest")
            .post(goboMockedUrl)
            .body(ElFileBody("booberVault_query.json"))
            .check(jsonPath("$.errors").notExists())
    )

    val httpProtocol =
        http.baseUrl(goboUrl)
            .contentTypeHeader("application/json")
            .authorizationHeader("Bearer $token")

    init {
        setUp(
            listOf(
                deleteToxics.injectOpen(rampUsers(1).during(10)),
                addToxics.injectOpen(nothingFor(Duration.ofSeconds(10)), rampUsers(1).during(10)),
                booberVaultScenario.injectOpen(nothingFor(Duration.ofSeconds(20)), rampUsersPerSec(1.0).to(6.0).during(Duration.ofMinutes(15)))
                //  databaseSchemaScenario.injectOpen(nothingFor(Duration.ofSeconds(20)), rampUsersPerSec(1.0).to(6.0).during(Duration.ofMinutes(15)))
            ),
        ).protocols(httpProtocol)
    }
}
