import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt

class GoboLoadSimulation extends Simulation {

  private val token = System.getenv("token")
  private val goboMockedUrl = "https://m78879-gobo-aup.apps.utv01.paas.skead.no/graphql";
  private val goboUrl = "https://gobo-aup.apps.utv01.paas.skead.no/graphql";

  private val httpProtocol = http
    .baseUrl(goboUrl)
    .contentTypeHeader("application/json")
    .authorizationHeader(s"Bearer $token")

  private val usageScenario = scenario("GoboUsage")
    .exec(
      http("usageRequest")
        .post("/graphql")
        .body(ElFileBody("usage.json"))
        .check(status.is(200))
    ).exitHereIfFailed

  private val affiliationsScenario = scenario("Affiliations")
    .exec(
      http("affiliationRequest")
        .post("/graphql")
        .body(ElFileBody("affiliations_query.json"))
        .check(jsonPath("$.errors").notExists)
    ).exitHereIfFailed

  private val databaseSchemaScenario = scenario("Affiliations")
    .exec(
      http("databasSchemaRequest")
        .post(goboMockedUrl)
        .body(ElFileBody("databaseSchemas_query.json"))
        .check(jsonPath("$.errors").notExists)
    ).exitHereIfFailed

  private val deleteMokeyLatencyToxic = scenario("Mokey-deletelatencytoxic")
    .exec(
      http("mokeyDeleteLatencyToxicRequest")
        .post(goboUrl)
        .body(ElFileBody("mokey_delete_latency_toxic_mutation.json"))
    )

  private val deleteMokeyTimeoutToxic = scenario("Mokey-deletetimeouttoxic")
    .exec(
      http("mokeyDeleteTimeoutToxicRequest")
        .post(goboUrl)
        .body(ElFileBody("mokey_delete_timeout_toxic_mutation.json"))
    )

  private val deleteMokeyDelayToxic = scenario("Mokey-deletedelaytoxic")
    .exec(
      http("mokeyDeleteDelayToxicRequest")
        .post(goboUrl)
        .body(ElFileBody("mokey_delete_delay_toxic_mutation.json"))
    )

  private val addMokeyLatencyToxic = scenario("Mokey-addlatencytoxic")
    .exec(
      http("mokeyAddLatencyToxicRequest")
        .post(goboUrl)
        .body(ElFileBody("mokey_add_latency_toxic_mutation.json"))
        .check(jsonPath("$.errors").notExists)
    ).exitHereIfFailed

  private val addMokeyTimoutToxic = scenario("Mokey-addtimeouttoxic")
    .exec(
      http("mokeyAddTimeoutToxicRequest")
        .post(goboUrl)
        .body(ElFileBody("mokey_add_timeout_toxic_mutation.json"))
        .check(jsonPath("$.errors").notExists)
    ).exitHereIfFailed

  private val addMokeyDelayToxic = scenario("Mokey-adddelaytoxic")
    .exec(
      http("mokeyAddDelayToxicRequest")
        .post(goboUrl)
        .body(ElFileBody("mokey_add_delay_toxic_mutation.json"))
        .check(jsonPath("$.errors").notExists)
    ).exitHereIfFailed

  private val userSettingsScenario = scenario("UserSettings")
    .exec(
      http("userSettingsRequest")
        .post("/graphql")
        .body(ElFileBody("userSettings.json"))
        .check(status.is(200))
    ).exitHereIfFailed

  val delete = scenario("delete")
    .exec(deleteMokeyLatencyToxic.exec())
    .exec(deleteMokeyTimeoutToxic.exec())
    .exec(deleteMokeyDelayToxic.exec())

  val add = scenario("Add")
    .exec(addMokeyLatencyToxic.exec())
    .exec(addMokeyTimoutToxic.exec())
    .exec(addMokeyDelayToxic.exec())

  setUp(
    delete.inject(atOnceUsers(1)),
    add.inject(nothingFor(10.seconds),atOnceUsers(1)),
      // Clean up previous toxics.

    // Run the test.
    //usageScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes)),
    //affiliationsScenario.inject(rampUsersPerSec(1).to(5).during(1.minutes)),
    databaseSchemaScenario.inject(nothingFor(10.seconds), rampUsersPerSec(1).to(6).during(15.minutes)),
   //databaseSchemaScenario.inject(nothingFor(1.seconds), rampUsersPerSec(1).to(5).during(15.minutes).randomized),
    //databaseSchemaScenario.inject(nothingFor(1.seconds), constantUsersPerSec(5).during(15.minutes).randomized),
    // userSettingsScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes))
  ).protocols(httpProtocol)
}