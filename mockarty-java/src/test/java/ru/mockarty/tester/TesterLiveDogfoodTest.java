// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Dogfood of the Java Tester DSL (the framework we sell) against a LIVE Mockarty
 * admin — mirrors the Go sdk_dogfood_tester_dsl and the Python live dogfood.
 * Seeds real mocks via the admin REST, then points the fluent facets at them.
 *
 * <p>Skipped unless {@code MOCKARTY_DOGFOOD_SERVER} is set (e.g.
 * http://127.0.0.1:5970). Uses the always-present "sandbox" namespace + uuid
 * route prefixes (a fresh namespace hits the SQLite trial cap).</p>
 */
public class TesterLiveDogfoodTest {

    private static final ObjectMapper M = new ObjectMapper();
    private static String server;
    private static HttpClient http;
    private static String token;

    @BeforeAll
    static void setUp() throws Exception {
        server = System.getenv("MOCKARTY_DOGFOOD_SERVER");
        assumeTrue(server != null && !server.isEmpty(),
                "set MOCKARTY_DOGFOOD_SERVER to dogfood against a live admin");
        server = server.replaceAll("/+$", "");
        http = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(10)).build();
        post("/api/v1/auth/login", "{\"login\":\"admin\",\"password\":\"admin\"}", false);
        HttpResponse<String> tr = post("/api/v1/auth/tokens",
                "{\"name\":\"javadf-" + UUID.randomUUID().toString().substring(0, 8) + "\"}", false);
        token = M.readTree(tr.body()).path("token").asText();
        assumeTrue(!token.isEmpty(), "could not provision an API token");
    }

    private static HttpResponse<String> post(String path, String json, boolean auth) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(server + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (auth && token != null) { b.header("Authorization", "Bearer " + token); }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String rt(String suffix) {
        return "/jdf/" + UUID.randomUUID().toString().replace("-", "").substring(0, 10) + suffix;
    }

    @Test
    void httpChainAndSseAgainstLiveMocks() throws Exception {
        String ns = "sandbox";
        String base = rt("");
        post("/api/v1/mocks", "{\"namespace\":\"" + ns + "\",\"http\":{\"route\":\"" + base + "/login\",\"httpMethod\":\"GET\"},\"response\":{\"statusCode\":200,\"headers\":{\"Content-Type\":[\"application/json\"]},\"payload\":{\"token\":\"tok-xyz\",\"user\":\"alice\"}}}", true);
        post("/api/v1/mocks", "{\"namespace\":\"" + ns + "\",\"http\":{\"route\":\"" + base + "/me\",\"httpMethod\":\"GET\",\"headers\":[{\"path\":\"Authorization\",\"assertAction\":\"equals\",\"value\":\"Bearer tok-xyz\"}]},\"response\":{\"statusCode\":200,\"headers\":{\"Content-Type\":[\"application/json\"]},\"payload\":{\"name\":\"Alice\",\"id\":42}}}", true);
        post("/api/v1/mocks", "{\"namespace\":\"" + ns + "\",\"sse\":{\"eventPath\":\"" + base + "/events\",\"eventName\":\"updates\"},\"response\":{\"statusCode\":200,\"sseEventChain\":{\"events\":[{\"eventName\":\"updates\",\"data\":{\"status\":\"connected\",\"seq\":1}},{\"eventName\":\"updates\",\"data\":{\"status\":\"running\",\"seq\":2}}]}}}", true);
        Thread.sleep(300);

        String stub = server + "/stubs/" + ns + base;
        Tester t = new Tester.Builder().build();
        t.http().get(stub + "/login")
                .expectStatus(200).expectJsonPath("$.user", "alice").extract("$.token", "tok");
        t.http().get(stub + "/me")
                .header("Authorization", "Bearer {{tok}}")
                .expectStatus(200).expectJsonPath("$.name", "Alice").extract("$.id", "uid");
        t.sse(stub + "/events").subscribe()
                .listen(Duration.ofSeconds(4))
                .expectMinEvents(2).expectEvent("updates")
                .expectJsonPath("updates", "$.status", "connected")
                .extract("updates", "$.status", "first")
                .done();
        t.finish();

        assertTrue(t.ok(), () -> "java tester DSL failed against live mocks: " + t.errors());
        assertEquals("42", t.vars().get("uid"));
        assertEquals("connected", t.vars().get("first"));
    }

    @Test
    void graphqlAgainstLiveMock() throws Exception {
        String ns = "sandbox";
        String route = rt("/graphql");
        post("/api/v1/mocks", "{\"namespace\":\"" + ns + "\",\"pathPrefix\":\"" + route + "\",\"graphql\":{\"operation\":\"query\",\"field\":\"user\"},\"response\":{\"statusCode\":200,\"headers\":{\"Content-Type\":[\"application/json\"]},\"payload\":{\"data\":{\"user\":{\"id\":\"u-graphql-1\",\"name\":\"Mockarty\"}}}}}", true);
        Thread.sleep(200);

        Tester t = new Tester.Builder().build();
        t.graphql(server + "/stubs/" + ns + route)
                .query("query { user { id name } }", null)
                .expectStatus(200)
                .expectNoErrors()
                .expectField("$.data.user.id", "u-graphql-1")
                .extract("$.data.user.name", "uname")
                .done();
        t.finish();

        assertTrue(t.ok(), () -> "java GraphQL DSL failed against live mock: " + t.errors());
        assertEquals("Mockarty", t.vars().get("uname"));
    }

    @Test
    void soapAgainstLiveMock() throws Exception {
        String ns = "sandbox";
        String route = rt("/soap/calc");
        String respXml = "<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\"?>"
                + "<soap:Envelope xmlns:soap=\\\"http://schemas.xmlsoap.org/soap/envelope/\\\">"
                + "<soap:Body><AddResponse xmlns=\\\"urn:Calc\\\"><result>5</result></AddResponse></soap:Body>"
                + "</soap:Envelope>";
        post("/api/v1/mocks", "{\"namespace\":\"" + ns + "\",\"pathPrefix\":\"" + route + "\",\"soap\":{\"path\":\"" + route + "\",\"service\":\"Calc\",\"method\":\"Add\",\"action\":\"urn:Calc/Add\"},\"response\":{\"statusCode\":200,\"headers\":{\"Content-Type\":[\"text/xml; charset=utf-8\"]},\"payload\":\"" + respXml + "\"}}", true);
        Thread.sleep(200);

        Tester t = new Tester.Builder().build();
        t.soap(server + "/stubs/" + ns + route)
                .call("urn:Calc/Add", "<Add xmlns=\"urn:Calc\"><a>2</a><b>3</b></Add>")
                .expectStatus(200)
                .expectNoFault()
                .expectXPathContains("//*[local-name()='result']", "5")
                .done();
        t.finish();

        assertTrue(t.ok(), () -> "java SOAP DSL failed against live mock: " + t.errors());
    }
}
