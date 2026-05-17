// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Wire-shape regression tests for Page&lt;T&gt;.
 *
 * <p>Surfaced 2026-05-17 by a sibling-instance hunt after the
 * SaveMockResponse.isNew cross-SDK fix. The admin emits resource-specific
 * collection keys ({@code mocks}, {@code tags}, {@code plans}, ...)
 * rather than a uniform {@code items}, so Page&lt;T&gt; needs
 * {@code @JsonAlias} on its {@code items} field to keep listing
 * endpoints working.
 */
class PageDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules();

    @Test
    void decodesCanonicalMocksEnvelope() throws Exception {
        String json = "{\"mocks\":[{\"id\":\"m1\"},{\"id\":\"m2\"}]," +
                "\"count\":42,\"limit\":50," +
                "\"message\":\"Mock list retrieved successfully\"}";

        Page<Mock> page = mapper.readValue(json,
                new TypeReference<Page<Mock>>() {});

        assertEquals(2, page.getItems().size(),
                "items must come from 'mocks' alias on the wire");
        assertEquals("m1", page.getItems().get(0).getId());
        assertEquals(42L, page.getTotal(),
                "total must come from 'count' alias on the wire");
        assertEquals(50, page.getLimit());
    }

    @Test
    void decodesLegacyItemsTotalEnvelope() throws Exception {
        // A downgraded server still emitting the original {items,total}
        // shape must decode for forward-compat.
        String json = "{\"items\":[{\"id\":\"x\"}],\"total\":1,\"offset\":0,\"limit\":50}";

        Page<Mock> page = mapper.readValue(json,
                new TypeReference<Page<Mock>>() {});

        assertEquals(1, page.getItems().size());
        assertEquals("x", page.getItems().get(0).getId());
        assertEquals(1L, page.getTotal());
    }

    @Test
    void decodesEmptyEnvelopeCleanly() throws Exception {
        String json = "{\"mocks\":[],\"count\":0,\"limit\":50}";

        Page<Mock> page = mapper.readValue(json,
                new TypeReference<Page<Mock>>() {});

        assertNotNull(page.getItems());
        assertEquals(0, page.getItems().size());
        assertEquals(0L, page.getTotal());
    }

    @Test
    void decodesTagsEnvelope() throws Exception {
        // GET /api/v1/tags emits {"tags":[...], "namespace":"..."} —
        // proves the alias list covers more than mocks.
        String json = "{\"tags\":[\"smoke\",\"perf\"],\"namespace\":\"sandbox\"}";

        Page<String> page = mapper.readValue(json,
                new TypeReference<Page<String>>() {});

        assertEquals(2, page.getItems().size());
        assertEquals("smoke", page.getItems().get(0));
    }
}
