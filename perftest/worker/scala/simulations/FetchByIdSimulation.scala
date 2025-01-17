package simulations


import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

// This simulation assumes there are 100k artifacts already added to the storage.
class FetchByIdSimulation extends Simulation {

  val registryUrl = scala.util.Properties.envOrElse("REGISTRY_URL", "http://localhost:8080/apis/registry/v2")
  val users = scala.util.Properties.envOrElse("TEST_USERS", "100").toInt
  val ramp = scala.util.Properties.envOrElse("TEST_RAMP_TIME", "60").toInt

  val httpProtocol = http
    .baseUrl(registryUrl)
    .acceptHeader("text/html,application/xhtml+xml,application/json,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val rando = new java.security.SecureRandom()

  val scn = scenario("Fetch By Id Test") // A scenario is a chain of requests and pauses
    .repeat(1000)(
      exec(session => session.set("idx", "" + (rando.nextInt(90000) + 100)))
      .exec(http("get_latest_version")
        .get("/groups/default/artifacts/TestArtifact-${idx}")
      )
      .pause(1)

      .exec(http("get_latest_metadata")
        .get("/groups/default/artifacts/TestArtifact-${idx}/meta")
        .check(jsonPath("$.globalId").saveAs("globalId"))
      )
      .pause(1)

      .exec(http("get_by_globalId")
        .get("/ids/globalIds/${globalId}")
      )
      .pause(1)
      .exec(http("get_by_version")
        .get("/groups/default/artifacts/TestArtifact-${idx}/versions/1")
      )
      .pause(1)
    )

  setUp(
      scn.inject(rampUsers(300) during (300 seconds))
  ).protocols(httpProtocol)
}
