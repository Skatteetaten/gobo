import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt

class GoboLoadSimulation extends Simulation {

  private val token = System.getenv("token")
  private val httpProtocol = http
    .baseUrl("https://m78879-gobo-aup.apps.utv01.paas.skead.no")
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
        .check(status.is(200))
    ).exitHereIfFailed

  private val databaseSchemaScenario = scenario("Affiliations")
    .exec(
      http("databasSchemaRequest")
        .post("/graphql")
        .body(ElFileBody("databaseSchemas_query.json"))
        .check(status.is(200))
    ).exitHereIfFailed

  private val userSettingsScenario = scenario("UserSettings")
    .exec(
      http("userSettingsRequest")
        .post("/graphql")
        .body(ElFileBody("userSettings.json"))
        .check(status.is(200))
    ).exitHereIfFailed

  setUp(
    //usageScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes)),
    //affiliationsScenario.inject(rampUsersPerSec(1).to(5).during(1.minutes)),
    databaseSchemaScenario.inject(rampUsersPerSec(1).to(5).during(1.minutes)),
    // userSettingsScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes))
  ).protocols(httpProtocol)
}