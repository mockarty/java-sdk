// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.
package ru.mockarty.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionAssertionCompatibilityTest {
    @Test
    @SuppressWarnings("unchecked")
    void collectionAssertionRetainsSkipped() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue("{\"assertions\":[{\"name\":\"optional\",\"passed\":false,\"skipped\":true}]}", Map.class);
        Map<String, Object> assertion = ((List<Map<String, Object>>) result.get("assertions")).get(0);
        assertEquals(Boolean.TRUE, assertion.get("skipped"));
        Map<String, Object> roundTrip = mapper.readValue(mapper.writeValueAsString(result), Map.class);
        assertEquals(Boolean.TRUE, ((List<Map<String, Object>>) roundTrip.get("assertions")).get(0).get("skipped"));
    }
}
