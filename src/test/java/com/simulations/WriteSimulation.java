package com.simulations;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class WriteSimulation extends Simulation {

    String baseUrl = "http://localhost:8080";

    HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl);

    ChainBuilder writeOnly = exec(http("Create Short URL")
                    .post("/url/body")
                    .header("Content-Type", "text/plain")
                    .body(StringBody("example.com"))

            )
            .pause(Duration.ofMillis(100)); // ~10 RPS per user

    ScenarioBuilder scenario = scenario("Write Only Load Test")
            .during(Duration.ofMinutes(1)).on(writeOnly);

    {
        setUp(
                scenario.injectOpen(atOnceUsers(30)) // adjust users as needed
        ).protocols(httpProtocol);
    }
}
