// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfConfigOptionsTest {

    // Catches a regression where saved perf config options are flattened or
    // omitted, causing the server to ignore metrics-push settings.
    @Test
    void serializesMetricsPushInsideSavedOptionsEnvelope() throws Exception {
        PerfConfig config = new PerfConfig()
                .name("checkout soak")
                .script("export default function () {}")
                .options(new PerfOptions()
                        .vus(12)
                        .duration("2m")
                        .metricsPush(List.of("prometheus:https://metrics.example/push"))
                        .metricsPushInterval("10s")
                        .stages(List.of(new PerfStage().duration("30s").target(12))));

        JsonNode payload = new ObjectMapper().valueToTree(config);

        assertEquals("prometheus:https://metrics.example/push", payload.at("/options/metricsPush/0").asText());
        assertEquals("10s", payload.at("/options/metricsPushInterval").asText());
        assertEquals(12, payload.at("/options/stages/0/target").asInt());
    }

    @Test
    void getModelPutPreservesFutureFieldsAndCanonicalizesMaxVUs() throws Exception {
        String response = "{\"id\":\"cfg-1\",\"collectionId\":\"col-1\",\"parentId\":\"folder-1\","
                + "\"namespace\":\"payments\",\"userId\":\"user-1\",\"name\":\"nightly\","
                + "\"script\":\"export default function () {}\",\"sortOrder\":4,\"isFolder\":false,"
                + "\"environment\":{\"region\":\"eu\",\"attempt\":2},\"createdAt\":\"2026-08-22T10:00:00Z\","
                + "\"updatedAt\":\"2026-08-22T11:00:00Z\",\"futureConfig\":{\"keep\":true},"
                + "\"options\":{\"maxVus\":17,\"futureOption\":{\"keep\":true},"
                + "\"stages\":[{\"duration\":\"30s\"}],\"abortCriteria\":[{\"metric\":\"http_req_failed\"}]}}";
        ObjectMapper mapper = new ObjectMapper();

        PerfConfig config = mapper.readValue(response, PerfConfig.class);

        assertEquals("col-1", config.getCollectionId());
        assertEquals("folder-1", config.getParentId());
        assertEquals("user-1", config.getUserId());
        assertEquals(17, config.getOptions().getMaxVus());
        assertEquals(0, config.getOptions().getStages().get(0).getTarget());
        assertEquals(false, config.getOptions().getAbortCriteria().get(0).getEnabled());

        JsonNode payload = mapper.valueToTree(config);
        assertEquals("col-1", payload.at("/collectionId").asText());
        assertEquals("user-1", payload.at("/userId").asText());
        assertEquals(4, payload.at("/sortOrder").asInt());
        assertEquals(false, payload.at("/isFolder").asBoolean());
        assertEquals(true, payload.at("/futureConfig/keep").asBoolean());
        assertEquals(17, payload.at("/options/maxVUs").asInt());
        assertEquals(true, payload.at("/options/maxVus").isMissingNode());
        assertEquals(true, payload.at("/options/futureOption/keep").asBoolean());
    }

    @Test
    void canonicalMaxVUsWinsRegardlessOfOrderOrNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String wire : List.of(
                "{\"maxVUs\":23,\"maxVus\":7}",
                "{\"maxVus\":7,\"maxVUs\":23}")) {
            PerfOptions options = mapper.readValue(wire, PerfOptions.class);
            assertEquals(23, options.getMaxVus());
            JsonNode payload = mapper.valueToTree(options);
            assertEquals(23, payload.at("/maxVUs").asInt());
            assertTrue(payload.at("/maxVus").isMissingNode());
        }
        for (String wire : List.of(
                "{\"maxVUs\":null,\"maxVus\":7}",
                "{\"maxVus\":7,\"maxVUs\":null}")) {
            PerfOptions options = mapper.readValue(wire, PerfOptions.class);
            assertNull(options.getMaxVus());
            JsonNode payload = mapper.valueToTree(options);
            assertTrue(payload.at("/maxVUs").isMissingNode());
            assertTrue(payload.at("/maxVus").isMissingNode());
        }
    }

    @Test
    void extrasCannotInjectTypedNamesAndAccessorsAreImmutable() {
        PerfConfig config = new PerfConfig().name("typed-name");
        config.putExtra("name", "injected-name");
        config.putExtra("parentId", "injected-parent");
        config.putExtra("PARENTID", "case-injected-parent");
        config.putExtra("futureConfig", null);
        assertFalse(config.getExtra().containsKey("name"));
        assertFalse(config.getExtra().containsKey("parentId"));
        assertFalse(config.getExtra().containsKey("PARENTID"));
        assertTrue(config.getExtra().containsKey("futureConfig"));
        assertThrows(UnsupportedOperationException.class,
                () -> config.getExtra().put("name", "injected-through-getter"));

        PerfOptions options = new PerfOptions();
        for (String protectedName : List.of("metricsPush", "maxVUs", "maxVus", "MAXVUS")) {
            options.putExtra(protectedName, "injected");
            assertFalse(options.getExtra().containsKey(protectedName));
        }
        assertThrows(UnsupportedOperationException.class,
                () -> options.getExtra().put("maxVUs", 99));
    }

    @Test
    void nestedModelsPreserveUnknownFieldsAndRejectTypedExtraInjection() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String wire = "{\"stages\":[{\"duration\":\"30s\",\"futureStage\":null}],"
                + "\"abortCriteria\":[{\"metric\":\"http_req_failed\",\"futureCriterion\":{\"keep\":true}}]}";

        PerfOptions options = mapper.readValue(wire, PerfOptions.class);
        PerfStage stage = options.getStages().get(0);
        AbortCriterion criterion = options.getAbortCriteria().get(0);
        stage.putExtra("duration", "injected");
        stage.putExtra("targetRPS", 99);
        stage.putExtra("targetRps", 98);
        criterion.putExtra("enabled", true);
        assertThrows(UnsupportedOperationException.class,
                () -> stage.getExtra().put("duration", "injected-through-getter"));
        assertThrows(UnsupportedOperationException.class,
                () -> criterion.getExtra().put("enabled", true));

        JsonNode payload = mapper.valueToTree(options);
        assertEquals("30s", payload.at("/stages/0/duration").asText());
        assertTrue(payload.at("/stages/0/targetRPS").isMissingNode());
        assertTrue(payload.at("/stages/0/targetRps").isMissingNode());
        assertTrue(payload.at("/stages/0/futureStage").isNull());
        assertFalse(payload.at("/abortCriteria/0/enabled").asBoolean());
        assertTrue(payload.at("/abortCriteria/0/futureCriterion/keep").asBoolean());
    }
}
