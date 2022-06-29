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

  private val deleteMokeyToxic = scenario("Mokey-deletetoxic")
    .exec(
      http("mokeyDeleteToxicRequest")
        .post(goboUrl)
        .body(ElFileBody("mokey_delete_toxic_mutation.json"))
    )

  private val addMokeyToxic = scenario("Mokey-addtoxic")
    .exec(
      http("mokeyAddToxicRequest")
        .post(goboUrl)
        .body(ElFileBody("mokey_add_toxic_mutation.json"))
        .check(jsonPath("$.errors").notExists)
    ).exitHereIfFailed


  private val userSettingsScenario = scenario("UserSettings")
    .exec(
      http("userSettingsRequest")
        .post("/graphql")
        .body(ElFileBody("userSettings.json"))
        .check(status.is(200))
    ).exitHereIfFailed

  setUp(
    deleteMokeyToxic.inject(atOnceUsers(1)),
    addMokeyToxic.inject( nothingFor(500.millis), atOnceUsers(1)),
    //usageScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes)),
    //affiliationsScenario.inject(rampUsersPerSec(1).to(5).during(1.minutes)),
    databaseSchemaScenario.inject(nothingFor(1.seconds), rampUsersPerSec(1).to(10).during(3.minutes)),
    // userSettingsScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes))
  ).protocols(httpProtocol)
}