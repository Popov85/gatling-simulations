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

    HttpProtocolBuilder httpProtocol = http.baseUrl("http://localhost:8080");

    // Step 1: Generate 1000 short codes per virtual user
    ChainBuilder createUrls = exec(session -> session.set("shortURLs", new ArrayList<String>()))
            .repeat(10, "i").on(
                    exec(session -> {
                        String longUrl = "https://example.com/" + UUID.randomUUID();
                        return session.set("longURL", longUrl);
                    })
                            .exec(http("Create Short URL")
                                    .post("/url")
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
                    .pause(Duration.ofMillis(1000)) // ~1 RPS per user
    );

    ScenarioBuilder scenario = scenario("Write then Read Test")
            .exec(createUrls)
            .pause(2)
            .exec(resolveUrls);

    {
        setUp(scenario.injectOpen(atOnceUsers(1)) // one user generates 1000 and reads them
        // scenario.injectOpen(atOnceUsers(10)) // Each of 10 users does 1000 POST + 10-min read
        ).protocols(httpProtocol);
    }
}
