import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt

class GoboLoadSimulation extends Simulation {

  private val token = System.getenv("token")
  private val httpProtocol = http
    .baseUrl("https://k82814-gobo-aurora.utv.paas.skead.no")
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
        .body(ElFileBody("affiliations.json"))
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
    usageScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes)),
    // affiliationsScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes)),
    // userSettingsScenario.inject(rampUsersPerSec(10).to(50).during(10.minutes))
  ).protocols(httpProtocol)
}