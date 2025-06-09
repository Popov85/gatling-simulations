package com.simulations;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class ShortenerSimulation extends Simulation {

    String baseUrl = "http://localhost:8080";

    HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl);

    // Step 1: Generate 1000 short codes per virtual user
    ChainBuilder createUrls = exec(session -> session.set("shortURLs", new ArrayList<String>()))
            .repeat(100, "i").on(
                    exec(session -> {
                        String longUrl = "https://example.com/" + UUID.randomUUID();
                        return session.set("longURL", longUrl);
                    })
                            .exec(http("Create Short URL")
                                    .post("/url/body")
                                    .basicAuth("user", "user")
                                    .header("Content-Type", "text/plain")
                                    .body(StringBody("#{longURL}"))
                                    .check(bodyString().saveAs("shortURL")) // save the full short URL response
                            )
                            .exec(session -> {
                                List<String> list = session.getList("shortURLs");
                                list.add(session.getString("shortURL"));
                                return session.set("shortURL", list);
                            })
                            .pause(Duration.ofMillis(100))
            );

    // Step 2: Pick random short code from session and GET /r/{short_code}
    ChainBuilder resolveUrls = during(Duration.ofMinutes(1)).on(
            exec(session -> {
                List<String> urls = session.getList("shortURLs");
                String randomURL = urls.get((int) (Math.random() * urls.size()));
                return session.set("randomURL", randomURL);
            })
                    .exec(http("Resolve Short URL")
                            .get("#{randomURL}")
                            .disableFollowRedirect() // <-- prevents Gatling from following Location
                            .check(status().is(302))
                    )
                    .pause(Duration.ofMillis(100)) // ~10 RPS per user
    );

    ScenarioBuilder scenario = scenario("Write then Read Test")
            .exec(createUrls)
            .pause(2)
            .exec(resolveUrls);

    {
        setUp(scenario.injectOpen(atOnceUsers(30)) // Each of X users does N POST + M-min read
                //setUp(scenario.injectOpen(rampUsers(10).during(Duration.ofSeconds(60))) // smooth load, over 30 sec
        ).protocols(httpProtocol);
    }
}
