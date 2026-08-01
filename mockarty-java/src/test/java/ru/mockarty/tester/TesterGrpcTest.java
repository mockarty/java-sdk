// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the gRPC facet against an in-memory fake invoker (no real
 * gRPC server / protobuf reflection needed). Mirrors the Go/Python port.
 */
public class TesterGrpcTest {

    private static final GrpcFacet.GrpcInvoker FAKE = (fullMethod, req) -> {
        if (fullMethod.endsWith("/Get")) {
            Map<String, Object> auth = new LinkedHashMap<>();
            auth.put("token", "abc");
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("name", "Alice");
            r.put("id", 42);
            r.put("auth", auth);
            return r;
        }
        throw new RuntimeException("NOT_FOUND: no such user");
    };

    @Test
    void grpcOkExpectExtract() {
        Tester t = new Tester.Builder().build();
        t.grpc(FAKE).call("user.UserService/Get", Map.of("id", 42))
                .expectOk()
                .expectField("$.name", "Alice")
                .expectJsonPath("$.id", 42)
                .extract("$.auth.token", "tok")
                .done();
        t.finish();
        assertTrue(t.ok(), () -> "errors: " + t.errors());
        assertEquals("abc", t.vars().get("tok"));
    }

    @Test
    void grpcExpectError() {
        Tester t = new Tester.Builder().build();
        t.grpc(FAKE).call("user.UserService/Missing", null)
                .expectError()
                .done();
        t.finish();
        assertTrue(t.ok(), () -> "errors: " + t.errors()); // the error was expected
    }

    @Test
    void grpcExpectOkOnErrorFails() {
        Tester t = new Tester.Builder().build();
        t.grpc(FAKE).call("user.UserService/Missing", null)
                .expectOk()
                .done();
        t.finish();
        assertFalse(t.ok());
    }
}
