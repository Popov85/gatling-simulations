package com.simulations;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class ReadSimulation extends Simulation {

    String baseUrl = "http://localhost:8080";

    HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl);

    ChainBuilder readOnly = during(Duration.ofMinutes(1)).on(
            exec(http("Resolve Short URL")
                            .get("/r/hello")
                            .disableFollowRedirect() // <-- prevents Gatling from following Location
                            .check(status().is(302))
                    )
                    .pause(Duration.ofMillis(100)) // ~10 RPS per user
    );

    ScenarioBuilder scenario = scenario("Read Only Load Test")
            .during(Duration.ofMinutes(3)).on(readOnly);

    {
        setUp(
                scenario.injectOpen(atOnceUsers(400)) // adjust users as needed
        ).protocols(httpProtocol);
    }
}
