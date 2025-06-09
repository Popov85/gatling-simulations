package com.simulations;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class WriteRandomBodySimulation extends Simulation {

    String baseUrl = "http://localhost:8080";

    HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl).disableWarmUp().shareConnections();

    ChainBuilder writeOnly = feed(() -> Stream.generate(() -> Map.<String, Object>of(
                    "randomUrl", "https://example.com/" + UUID.randomUUID())).iterator())
            .exec(http("Create Short URL")
                    .post("/url/body")
                    .header("Content-Type", "text/plain")
                    .body(StringBody("#{randomUrl}"))
    ).pause(Duration.ofMillis(100)); // ~10 RPS per user

    ScenarioBuilder scenario = scenario("Write Only Load Test")
            .during(Duration.ofMinutes(15)).on(writeOnly);

    {
        setUp(
                scenario.injectOpen(atOnceUsers(1000)) // adjust users as needed
        ).protocols(httpProtocol);
    }
}
