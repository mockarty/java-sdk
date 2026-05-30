// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses message-pact JSON documents (V3 + V4 union) into {@link MessagePact.Message}. */
public final class MessagePactParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_OF_STRING_OBJECT =
        new TypeReference<>() {};

    private MessagePactParser() {}

    public static List<MessagePact.Message> parse(byte[] raw) {
        JsonNode root;
        try {
            root = MAPPER.readTree(raw);
        } catch (IOException e) {
            throw new IllegalArgumentException("message pact is not valid JSON: " + e.getMessage(), e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("message pact root must be a JSON object");
        }
        List<MessagePact.Message> out = new ArrayList<>();
        // V4: interactions with type=Asynchronous/Messages
        JsonNode ix = root.path("interactions");
        if (ix.isArray()) {
            for (JsonNode n : ix) {
                if (!n.isObject()) continue;
                JsonNode t = n.path("type");
                if (!t.isTextual() || !MessagePact.MESSAGE_INTERACTION_TYPE.equals(t.asText())) continue;
                out.add(decodeV4(n));
            }
        }
        // V3: top-level 'messages' array
        JsonNode msgs = root.path("messages");
        if (msgs.isArray()) {
            for (JsonNode n : msgs) {
                if (n.isObject()) out.add(decodeV3(n));
            }
        }
        return out;
    }

    private static MessagePact.Message decodeV4(JsonNode ix) {
        MessagePact.Message m = new MessagePact.Message();
        m.description = ix.path("description").asText("");
        m.states = decodeStates(ix);
        JsonNode contents = ix.path("contents");
        if (contents.isObject()) {
            m.contentType = contents.path("contentType").asText("");
            m.content = MAPPER.convertValue(contents.path("content"), Object.class);
        }
        JsonNode meta = ix.path("metadata");
        if (meta.isObject()) {
            Map<String, Object> mm = MAPPER.convertValue(meta, MAP_OF_STRING_OBJECT);
            for (Map.Entry<String, Object> e : mm.entrySet()) {
                if (e.getValue() instanceof String s) m.metadata.put(e.getKey(), s);
            }
        }
        return m;
    }

    private static MessagePact.Message decodeV3(JsonNode mr) {
        MessagePact.Message m = new MessagePact.Message();
        m.description = mr.path("description").asText("");
        m.states = decodeStates(mr);
        m.content = MAPPER.convertValue(mr.path("contents"), Object.class);
        JsonNode meta = mr.path("metaData");
        if (!meta.isObject()) meta = mr.path("metadata");
        if (meta.isObject()) {
            Map<String, Object> mm = MAPPER.convertValue(meta, MAP_OF_STRING_OBJECT);
            for (Map.Entry<String, Object> e : mm.entrySet()) {
                if (e.getValue() instanceof String s) m.metadata.put(e.getKey(), s);
            }
        }
        return m;
    }

    private static List<Map<String, Object>> decodeStates(JsonNode ix) {
        List<Map<String, Object>> out = new ArrayList<>();
        JsonNode arr = ix.path("providerStates");
        if (arr.isArray()) {
            for (JsonNode s : arr) {
                if (s.isObject()) {
                    out.add(MAPPER.convertValue(s, MAP_OF_STRING_OBJECT));
                }
            }
            return out;
        }
        JsonNode single = ix.path("providerState");
        if (single.isTextual() && !single.asText().isBlank()) {
            Map<String, Object> ss = new LinkedHashMap<>();
            ss.put("name", single.asText());
            out.add(ss);
        }
        return out;
    }
}
